package com.bytestorm.isp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;

import com.bytestorm.isp.MonthlyPivotReport.Summary;
import com.bytestorm.isp.WorkbookStyle.CellStyleType;
import com.bytestorm.isp.WorkbookStyle.FontType;

public class Main {

    public static void main(String[] args) {
        final HelpFormatter help = new HelpFormatter();
        final Options options = new Options();
        final CommandLineParser parser = new PosixParser();
        options.addOption("v", "verbose", false, "verbose debug messages");
        options.addOption("L", "local-dir", true, "use local files from specified directory instead of GCS");
        options.addOption("D", "date", true, "raport date in format yyyy.MM or yyyyMM, only used if local-dir is not defined");
        options.addOption("o", "output", true, "output file name");        
        options.addOption("C", "config", true, "properties files with style configuration");
        options.addOption("h", "help", false, "extended help");
        options.addOption(null, "no-overwrite", false, "prevents output file overwrite");
        options.addOption(null, "gcs-bucket", true, "GCS bucked for Play reports");
        options.addOption(null, "gcs-client-secret", true, "Google API client secret json file");
        options.addOption(null, "gcs-service-cert", true, "Google API service account access cert PK12 file");
        options.addOption(null, "gcs-service-email", true, "Google API service account e-mail address");        
        options.addOption(null, "no-xchange-sheet", false, "disables xchange sheet output (no xchange data will be fetched)");
        options.addOption(null, "no-summary-sheet", false, "disables summary sheet output");
        options.addOption(null, "no-vat-sheet", false, "disables vat sheet output");
        options.addOption(null, "no-vat", false, "disables VAT data processing (sales reports will not be used, implies no-vat-sheet)");
        help.setWidth(80);
        try {            
            final CommandLine cli = parser.parse(options, args);
            if (cli.hasOption('h')) {
                help.printHelp("java -jar isp [OPTIONS]...", options);
                System.out.println(CFG_TEMPLATE);
                System.exit(0);
            }
            Log.setVerbose(cli.hasOption('v'));
            final File reportsDir;
            if (cli.hasOption('L')) {
                reportsDir = new File(cli.getOptionValue('L'));
                if (!reportsDir.isDirectory()) {
                    System.err.println("Local dir path " + cli.getOptionValue('L') + " doesn't point to folder");
                    help.printHelp("java -jar isp [OPTIONS]...", options);
                    System.exit(-1);
                }
            } else {
                reportsDir = null;
            }
            final Configuration config = new Configuration();
            if (cli.hasOption('C')) {
                String cfgFilePath = cli.getOptionValue('C');
                if (null == cfgFilePath) {
                    System.err.println("Option --config lacks argument");
                    help.printHelp("java -jar isp [OPTIONS]...", options);
                    System.exit(-2);
                }
                try(FileInputStream in = new FileInputStream(cfgFilePath)) {
                    config.load(in);
                    Log.v("Unsing config file " + cfgFilePath);
                } catch (IOException ex) {
                    System.err.println("Cannot load configuration file " + cfgFilePath);
                    help.printHelp("java -jar isp [OPTIONS]...", options);
                    System.exit(-3);
                }
            } else {
                try(FileInputStream in = new FileInputStream("isp.properties")) {
                    config.load(in);
                    Log.v("Using default config file");
                } catch (Throwable ex) {
                    // skip silently
                }
            }
            File outFile = null;
            if (cli.hasOption('o')) {
                outFile = new File(cli.getArgs()[1]);
            }
            ReportsProvider reports;
            if (null == reportsDir) {
                // GCS mode - use google APIs lib to download reports from GCS storage (as defined in Play Console)
                Log.v("Running in GCS mode");
                if (cli.hasOption("gcs-client-secret")) {
                    config.setProperty("gcs.client.secret.json.path", cli.getOptionValue("gcs-client-secret"));
                }
                if (cli.hasOption("gcs-bucket")) {
                    config.setProperty("gcs.reports.bucket", cli.getOptionValue("gcs-bucket"));
                }
                if (cli.hasOption("gcs-service-cert")) {
                    config.setProperty("gcs.service.pk12.path", cli.getOptionValue("gcs-service-cert"));
                }
                if (cli.hasOption("gcs-service-email")) {
                    config.setProperty("gcs.service.email", cli.getOptionValue("gcs-service-email"));
                }
                Date date = null;
                if (cli.hasOption('D')) {
                    String val = cli.getOptionValue('D');
                    try {
                        date = DATE_FORMAT.parse(val.replace(".", ""));
                    } catch (java.text.ParseException ex) {
                        System.err.println("Invalid reports date  " + cli.getOptionValue('D'));
                        help.printHelp("java -jar isp [OPTIONS]...", options);
                        System.exit(-11);
                    }
                } else {
                    DateTime dt = new DateTime().minusMonths(1);
                    date = dt.toDate();
                }
                reports = new GCSReports(config, date);                
            } else {                     
                reports = new LocalReports(reportsDir);
            }
            // update config with CLI overrides
            if (cli.hasOption("no-vat")) {
                config.setBoolean("process.transactions.vat", false);
            }
            if (cli.hasOption("no-xchange-sheet")) {
                config.setBoolean("output.xchange.sheet", false);
            }
            if (cli.hasOption("no-vat-sheet")) {
                config.setBoolean("output.vat.sheet", false);
            }            
            if (cli.hasOption("no-summary-sheet")) {
                config.setBoolean("output.summary.sheet", false);
            }
            if (null == outFile) {
                outFile = new File(DATE_FORMAT.format(reports.getDate()) + ".xlsx");
            }
            Log.v("Report date: " + reports.getDate());
            for (File file : reports.getEarningsReportsFiles()) {
                Log.v("Earnings report file: " + file.getAbsolutePath());
            }
            for (File file : reports.getSalesReportsFiles()) {
                Log.v("Sales report file: " + file.getAbsolutePath());
            }
            if (cli.hasOption("no-overwrite")) {
                if (outFile.exists()) {
                    System.err.println("Output file " + outFile + " exists");
                    help.printHelp("java -jar isp [OPTIONS]... REPORTS_FOLDER [OUTPUT_FILE]", options);
                    System.exit(-4);
                }
            }
            // process transactions
            List<Transaction> transactions = parseInputCsvs(reports, !config.getBoolean("process.transactions.vat", true));
            final Date from = transactions.get(0).getDate();
            final Date to = transactions.get(transactions.size() - 1).getDate();
            if (config.getBoolean("output.xchange.sheet", true)) {                
                try {
                    final Exchange xchg = new Exchange(from, to);
                    xchg.download();                
                    for (Transaction t : transactions) {
                        final String buyerCurrency = t.getBuyerCurrency();                         
                        if (!buyerCurrency.equals(t.getMerchantCurrency())) {
                            Exchange.Quote xchgEntry = xchg.getQuote(t.getDate(), buyerCurrency);
                            t.setConversionRateBaseAmount(xchgEntry.getAmount());
                            t.setConversionRate(xchgEntry.getRate());
                        }                        
                    }
                } catch (Exception ex) {
                    System.err.println("Cannot download or parse NBP exchange data");
                    ex.printStackTrace();
                    System.exit(2);
                }
            }
            for (Transaction t : transactions) {
                final String buyerCurrency = t.getBuyerCurrency();                                                                    
                // currency format lookup init
                if (null == perCurrecyAmountFormat.get(buyerCurrency)) {
                    perCurrecyAmountFormat.put(buyerCurrency, CellStyleType.AMOUNT_SHORT);
                }
                // check if amount has fraction point
                if (!t.getAmount().subtract(t.getAmount().setScale(0, RoundingMode.FLOOR)).equals(BigDecimal.ZERO)) {
                    perCurrecyAmountFormat.put(buyerCurrency, CellStyleType.AMOUNT);
                }
            }
            try(FileOutputStream out = new FileOutputStream(outFile)) {
                Log.v("Creating output XLSX");
                final WorkbookStyle wb = new WorkbookStyle(new XSSFWorkbook(), config);
                createTransactionsSheet(wb, transactions, !config.getBoolean("process.transactions.vat", true));
                createPivotSheet(wb, transactions);
                if (config.getBoolean("process.transactions.vat", true) && config.getBoolean("output.vat.sheet", true)) {
                    createVatSheet(wb, transactions);
                }
                if (config.getBoolean("output.summary.sheet", true)) {
                    createSummarySheet(wb, transactions, !config.getBoolean("process.transactions.vat", true));
                }
                Log.v("Saving output XLSX file " + outFile);
                wb.getWorkbook().write(out);
            } catch (Exception ex) {
                System.err.println("Cannot write outut CSV file " + outFile);
                ex.printStackTrace();
                outFile.delete();
                System.exit(3);
            }           
        } catch (ParseException ex) {
            System.err.println("Command line parsing failed: " + ex.getMessage());
            help.printHelp("java -jar isp [OPTIONS]...", options);
            System.exit(4);
        } catch (IOException | IllegalArgumentException ex) {
            ex.printStackTrace();
            System.exit(5);
        }
    }
    
    private static List<Transaction> parseInputCsvs(ReportsProvider reports, boolean noVat) throws IOException {
        ArrayList<Transaction> retval = new ArrayList<>();
        HashMap<String, Integer> sellsBySku = new HashMap<>();
        HashMap<String, Integer> refundsBySku = new HashMap<>();
        HashSet<String> skus = new HashSet<>();
        HashMap<String, Transaction> transactionsLookup = new HashMap<>();
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal taxDeduction = BigDecimal.ZERO;
        File[] earningsReports = reports.getEarningsReportsFiles();        
        for (File file : earningsReports) {
            Log.v("Loading earnings CSV file " + file.getAbsolutePath());
            try(ICsvBeanReader beanReader = new CsvBeanReader(new FileReader(file), CsvPreference.STANDARD_PREFERENCE)) {            
                /* final String[] header = */ beanReader.getHeader(true); // header will be ignored                
                Transaction t;            
                while ((t = beanReader.read(Transaction.class, Transaction.MAPPING, Transaction.CSV)) != null) {
                    if (null == t.getTransactionType()) {
                        // tax deduction reports in some cases contains invalid rows with 0 payout
                        if (BigDecimal.ZERO.compareTo(t.getPayout()) == 0) {
                            continue;
                        }
                        throw new IOException("Invalid row " + beanReader.getRowNumber() 
                                + " - transaction don't have associated type and have value\n" + beanReader.getUntokenizedRow());
                    }
                    if (null == t.getId()) {
                        if (Transaction.Type.TAX != t.getTransactionType()) {
                            throw new IOException("Invalid row " + beanReader.getRowNumber() 
                                    + " - non-tax deduction transaction without id\n" + beanReader.getUntokenizedRow());
                        }
                        taxDeduction = taxDeduction.add(t.getPayout());
                    } else {
                        // initialize per SKU counters
                        String sku = t.getSkuId();
                        if (!sellsBySku.containsKey(sku)) {
                            sellsBySku.put(sku, 0);
                        }
                        if (!refundsBySku.containsKey(sku)) {
                            refundsBySku.put(sku, 0);
                        }
                        skus.add(sku);
                        if (Transaction.Type.CHARGE == t.getTransactionType()) {
                            sellsBySku.put(sku, sellsBySku.get(sku) + 1);
                            transactionsLookup.put(t.getId(), t);
                        } else if (Transaction.Type.REFUND == t.getTransactionType()) {
                            refundsBySku.put(sku, refundsBySku.get(sku) + 1);
                        }                        
                    }
                    income = income.add(t.getPayout());
                    retval.add(t);                    
                }
                Log.v("File loaded");
                Log.v("  total entries: " + retval.size());                
            }  
        }
        for (String sku : skus) {
            Log.v(sku + ":");
            Log.v("  - sells  : " + sellsBySku.get(sku));
            Log.v("  - refunds: " + refundsBySku.get(sku));
            Log.v(String.format("Intl tax deduction: %.02f PLN", taxDeduction.negate().floatValue()));
        }
        Log.v(String.format("Total income: %.02f PLN", income.floatValue()));
        Log.v("");
        Collections.sort(retval, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction o1, Transaction o2) {
                return Long.compare(o1.getDate().getTime(), o2.getDate().getTime());
            }
        });
        if (!noVat) {
            File[] salesReports = reports.getSalesReportsFiles();
            for (File file : salesReports) {
                Log.v("Loading sales report CSV file " + file.getAbsolutePath());
                try(ICsvBeanReader beanReader = new CsvBeanReader(new FileReader(file), CsvPreference.STANDARD_PREFERENCE)) {
                    /* final String[] header = */ beanReader.getHeader(true); // header will be ignored        
                    Sale sale;            
                    while ((sale = beanReader.read(Sale.class, Sale.MAPPING, Sale.CSV)) != null) {
                        final Transaction t = transactionsLookup.remove(sale.getId()); 
                        if (null == t) {
                            // this is valid case - sales are reported by charge date so some salles can be from previous month
                            continue;
                        }                    
                        // check if sales report and earnings report contains same data
                        if (0 != t.getAmount().compareTo(sale.getPrice())) {
                            throw new IOException("Prices differs in reports earnings: " + t.getAmount() + " sales: " + sale.getPrice());
                        }
                        if (!t.getBuyerCurrency().equals(sale.getBuyerCurrency())) {
                            throw new IOException("Currency differs in reports earnings: " + t.getBuyerCurrency() 
                                    + " sales: " + sale.getBuyerCurrency());
                        }
                        if (null != t.getBuyerCountry() && !t.getBuyerCountry().equals(sale.getBuyerCountry())) {
                            if (!t.getBuyerCurrency().equals(sale.getBuyerCurrency())) {
                                throw new IOException("Countries differs in reports earnings: " + t.getBuyerCountry() 
                                        + " sales: " + sale.getBuyerCountry() + " and currency is different");
                            }
                        }
                        // update transaction with extra data
                        t.setTaxAmount(sale.getTaxCollected());      
                    }
                }
            }
            if (!transactionsLookup.isEmpty()) {
                throw new IOException("Some transaction were not matched with transaction from sales reports");
            }
        }        
        return retval; 
    }    
    
    private static void createTransactionsSheet(WorkbookStyle wb, List<Transaction> transactions, boolean ignoreVat) {
        Log.v("Creating transactions sheet");
        final Sheet sheet = wb.getWorkbook().createSheet("Tranzakcje");
        final CellStyle centeredCellStyle = wb.cloneCellStyle(CellStyleType.BASE);
        centeredCellStyle.setAlignment(CellStyle.ALIGN_CENTER);
        int rowNbr = 0;
        // header
        Row row = sheet.createRow(rowNbr++);
        wb.appendCellWithStyle(row, "Identyfikator", CellStyleType.HEADER);
        wb.appendCellWithStyle(row, "Data", CellStyleType.HEADER);
        wb.appendCellWithStyle(row, "", CellStyleType.HEADER);
        wb.appendCellWithStyle(row, "Produkt", CellStyleType.HEADER);        
        wb.appendCellWithStyle(row, "Kraj", CellStyleType.HEADER);
        wb.appendCellWithStyle(row, "Waluta", CellStyleType.HEADER);
        wb.appendCellWithStyle(row, "Kwota", CellStyleType.HEADER);
        if (!ignoreVat) {
            wb.appendCellWithStyle(row, "VAT", CellStyleType.HEADER);
        }
        wb.appendCellWithStyle(row, "PLN", CellStyleType.HEADER);
        wb.appendCellWithStyle(row, "PLN(NBP)", CellStyleType.HEADER);
        wb.appendCellWithStyle(row, "Różnica", CellStyleType.HEADER);                
        for (Transaction t : transactions) {
            // skip tax deduction
            if (null == t.getId()) {
                continue;
            }
            row = sheet.createRow(rowNbr++);
            wb.appendCellWithStyle(row, t.getId(), centeredCellStyle);
            wb.appendCellWithStyle(row, t.getDate(), CellStyleType.DATE);
            wb.appendCellWithStyle(row, t.getTransactionType().toString(), CellStyleType.BASE);
            wb.appendCellWithStyle(row, t.getProductName(), CellStyleType.BASE);
            wb.appendCellWithStyle(row, t.getBuyerCountry(), centeredCellStyle);
            wb.appendCellWithStyle(row, t.getBuyerCurrency(), centeredCellStyle);
            wb.appendCellWithStyle(row, t.getAmount(), perCurrecyAmountFormat.get(t.getBuyerCurrency()));
            if (!ignoreVat) {
                if (Transaction.Type.CHARGE == t.getTransactionType() && EU_CURRENCIES.contains(t.getBuyerCurrency())) {
                    wb.appendCellWithStyle(row, t.getTaxAmount(), perCurrecyAmountFormat.get(t.getBuyerCurrency()));                    
                } else {
                    wb.appendCellWithStyle(row, "", CellStyleType.BASE);
                }
            }
            wb.appendCellWithStyle(row, t.getPayout(), CellStyleType.AMOUNT);
            wb.appendCellWithStyle(row, t.getAmountConverted(), CellStyleType.AMOUNT);
            wb.appendCellWithStyle(row, t.getSpread(), CellStyleType.AMOUNT);            
        }
        sheet.setRepeatingRows(CellRangeAddress.valueOf("1"));
        for (int i = 0; i <= (ignoreVat ? 9 : 10); i++) {
            sheet.autoSizeColumn(i);    
        }
    }
    
    private static void createPivotSheet(WorkbookStyle wb, List<Transaction> transactions) {
        Log.v("Creating pivot sheet");
        final Sheet sheet = wb.getWorkbook().createSheet("Zestawienie");
        final MonthlyPivotReport pivot = new MonthlyPivotReport(transactions);
        final Date[] days = pivot.getDays();
        final String[] currencies = pivot.getCurrencies();
        int rowNbr = 0;
        // header
        Row row = sheet.createRow(rowNbr++);
        wb.appendCellWithStyle(row, "Data", CellStyleType.HEADER);
        wb.appendCellWithStyle(row, "", CellStyleType.HEADER);
        for (String currency : currencies) {
            wb.appendCellWithStyle(row, currency, CellStyleType.HEADER);
        }
        final CellStyle lastColumnHeaderStyle = wb.cloneCellStyle(CellStyleType.HEADER);
        lastColumnHeaderStyle.setBorderLeft(CellStyle.BORDER_MEDIUM);
        wb.appendCellWithStyle(row, "Wynik", lastColumnHeaderStyle);
        // set of modified styles
        final CellStyle dateStyleTopStyle = wb.cloneCellStyle(CellStyleType.HEADER);
        final CellStyle dateStyleMiddleStyle = wb.cloneCellStyle(CellStyleType.HEADER);
        final CellStyle dateStyleBottomStyle = wb.cloneCellStyle(CellStyleType.HEADER);
        final CellStyle typeStyleTopStyle = wb.getWorkbook().createCellStyle();
        final CellStyle typeStyleMiddleStyle = wb.getWorkbook().createCellStyle();
        final CellStyle typeStyleBottomStyle = wb.getWorkbook().createCellStyle();
        final CellStyle amountTotalNormalStyle = wb.cloneCellStyle(CellStyleType.AMOUNT);
        final CellStyle amountTotalShortStyle = wb.cloneCellStyle(CellStyleType.AMOUNT_SHORT);
        final CellStyle amountMerchantCurrencyStyle = wb.cloneCellStyle(CellStyleType.AMOUNT);
        final CellStyle amountSpreadStyle = wb.cloneCellStyle(CellStyleType.AMOUNT);
        final CellStyle resultTotalStyle = wb.getWorkbook().createCellStyle();
        final CellStyle resultMerchantCurrencyStyle = wb.getWorkbook().createCellStyle();
        final CellStyle resultSpreadStyle = wb.getWorkbook().createCellStyle();        
        final CellStyle summaryLabelTopStyle = wb.cloneCellStyle(CellStyleType.HEADER);
        final CellStyle summaryLabelStyle = wb.cloneCellStyle(CellStyleType.HEADER);
        final CellStyle summaryTotalShortStyle = wb.cloneCellStyle(CellStyleType.AMOUNT_SHORT);
        final CellStyle summaryTotalNormalStyle = wb.cloneCellStyle(CellStyleType.AMOUNT);
        final CellStyle summaryStyle = wb.cloneCellStyle(CellStyleType.AMOUNT);
        
        
        // top cell in date column
        dateStyleTopStyle.setBorderBottom(CellStyle.BORDER_NONE);
        dateStyleTopStyle.setFont(wb.getFont(FontType.NORMAL));
        dateStyleTopStyle.setDataFormat(wb.getFormat("dd.MM.yy"));
        // middle cell in date column
        dateStyleMiddleStyle.setBorderTop(CellStyle.BORDER_NONE);
        dateStyleMiddleStyle.setBorderBottom(CellStyle.BORDER_NONE);
        dateStyleMiddleStyle.setFont(wb.getFont(FontType.NORMAL));
        // bottom cell i date column
        dateStyleBottomStyle.setBorderTop(CellStyle.BORDER_NONE);
        dateStyleBottomStyle.setFont(wb.getFont(FontType.NORMAL));
        
        // top cell in type name column
        typeStyleTopStyle.cloneStyleFrom(dateStyleTopStyle);
        typeStyleTopStyle.setAlignment(CellStyle.ALIGN_LEFT);
        // middle cell in type name column        
        typeStyleMiddleStyle.cloneStyleFrom(dateStyleMiddleStyle);
        typeStyleMiddleStyle.setAlignment(CellStyle.ALIGN_LEFT);
        // bottom cell in type name column        
        typeStyleBottomStyle.cloneStyleFrom(dateStyleBottomStyle);
        typeStyleBottomStyle.setAlignment(CellStyle.ALIGN_LEFT);
        
        // data display cell style (total row)
        amountTotalNormalStyle.setBorderBottom(CellStyle.BORDER_NONE);
        amountTotalShortStyle.setBorderBottom(CellStyle.BORDER_NONE);
        // data display cell style (payout and converted rows)
        amountMerchantCurrencyStyle.setBorderBottom(CellStyle.BORDER_NONE);
        amountMerchantCurrencyStyle.setBorderTop(CellStyle.BORDER_NONE);
        // data display cell style (spread row)
        amountSpreadStyle.setBorderTop(CellStyle.BORDER_NONE);
        
        // result column cell style
        resultTotalStyle.cloneStyleFrom(amountTotalNormalStyle);
        resultTotalStyle.setFont(wb.getFont(FontType.BOLD));
        resultTotalStyle.setBorderLeft(CellStyle.BORDER_MEDIUM);
        // result column cell style
        resultMerchantCurrencyStyle.cloneStyleFrom(amountMerchantCurrencyStyle);
        resultMerchantCurrencyStyle.setFont(wb.getFont(FontType.BOLD));
        resultMerchantCurrencyStyle.setBorderLeft(CellStyle.BORDER_MEDIUM);
        // result column cell style
        resultSpreadStyle.cloneStyleFrom(amountSpreadStyle);
        resultSpreadStyle.setFont(wb.getFont(FontType.BOLD));
        resultSpreadStyle.setBorderLeft(CellStyle.BORDER_MEDIUM);
        
        // summary (first) row label
        summaryLabelTopStyle.setAlignment(CellStyle.ALIGN_LEFT);
        summaryLabelTopStyle.setBorderTop(CellStyle.BORDER_MEDIUM);
        // summary row label
        summaryLabelStyle.setAlignment(CellStyle.ALIGN_LEFT);
        
        // summary (first) row cell
        summaryTotalShortStyle.setBorderTop(CellStyle.BORDER_MEDIUM);
        summaryTotalShortStyle.setFont(wb.getFont(FontType.NORMAL));
        summaryTotalNormalStyle.setBorderTop(CellStyle.BORDER_MEDIUM);
        summaryTotalNormalStyle.setFont(wb.getFont(FontType.NORMAL));
        // summary row cell
        summaryStyle.setFont(wb.getFont(FontType.NORMAL));
        
        for (Date date : days) {
            Row total = sheet.createRow(rowNbr++);
            Row payout = sheet.createRow(rowNbr++);
            Row converted = sheet.createRow(rowNbr++);
            Row spread = sheet.createRow(rowNbr++);
            // date column
            wb.appendCellWithStyle(total, date, dateStyleTopStyle);
            wb.appendCellWithStyle(payout, "", dateStyleMiddleStyle);
            wb.appendCellWithStyle(converted, "", dateStyleMiddleStyle);
            wb.appendCellWithStyle(spread, "", dateStyleBottomStyle);
            // cell column
            wb.appendCellWithStyle(total, "Kwota", typeStyleTopStyle);
            wb.appendCellWithStyle(payout, "PLN", typeStyleMiddleStyle);
            wb.appendCellWithStyle(converted, "PLN(NBP)", typeStyleMiddleStyle);
            wb.appendCellWithStyle(spread, "Różnica", typeStyleBottomStyle);
            
            // pivot data
            for (String currency : currencies) {
                final Summary summary = pivot.getDayCurrencySummary(date, currency);
                if (BigDecimal.ZERO.equals(summary.total)) {
                    wb.appendCellWithStyle(total, "", amountTotalNormalStyle);
                    wb.appendCellWithStyle(payout, "", amountMerchantCurrencyStyle);
                    wb.appendCellWithStyle(converted, "", amountMerchantCurrencyStyle);
                    wb.appendCellWithStyle(spread, "", amountSpreadStyle);
                } else {
                    if (CellStyleType.AMOUNT_SHORT == perCurrecyAmountFormat.get(currency)) {
                        wb.appendCellWithStyle(total, summary.total, amountTotalShortStyle);       
                    } else {
                        wb.appendCellWithStyle(total, summary.total, amountTotalNormalStyle);
                    }
                    wb.appendCellWithStyle(payout, summary.totalPayout, amountMerchantCurrencyStyle);
                    wb.appendCellWithStyle(converted, summary.totalConverted, amountMerchantCurrencyStyle);
                    wb.appendCellWithStyle(spread, summary.totalConverted.subtract(summary.totalPayout), amountSpreadStyle);
                }
            }
            final Summary summary = pivot.getDaySummary(date);
            // total column
            wb.appendCellWithStyle(total, "", resultTotalStyle);        
            wb.appendCellWithStyle(payout, summary.totalPayout, resultMerchantCurrencyStyle);
            wb.appendCellWithStyle(converted, summary.totalConverted, resultMerchantCurrencyStyle);
            wb.appendCellWithStyle(spread, summary.totalConverted.subtract(summary.totalPayout), resultSpreadStyle);            
        }
        sheet.autoSizeColumn(0);  
        sheet.autoSizeColumn(1);  
        
        // summary rows
        int summaryFirstRow = rowNbr;
        Row total = sheet.createRow(rowNbr++);
        Row payout = sheet.createRow(rowNbr++);
        Row converted = sheet.createRow(rowNbr++);
        Row spread = sheet.createRow(rowNbr++);
        wb.appendCellWithStyle(total, "Suma Kwota", summaryLabelTopStyle);
        wb.appendCellWithStyle(payout, "Suma PLN", summaryLabelStyle);
        wb.appendCellWithStyle(converted, "Suma PLN(NBP)", summaryLabelStyle);
        wb.appendCellWithStyle(spread, "Suma Różnica", summaryLabelStyle);
        wb.appendCellWithStyle(total, "", summaryLabelTopStyle);
        wb.appendCellWithStyle(payout, "", summaryLabelStyle);
        wb.appendCellWithStyle(converted, "", summaryLabelStyle);
        wb.appendCellWithStyle(spread, "", summaryLabelStyle);
        sheet.addMergedRegion(new CellRangeAddress(summaryFirstRow + 0, summaryFirstRow + 0, 0, 1));
        sheet.addMergedRegion(new CellRangeAddress(summaryFirstRow + 1, summaryFirstRow + 1, 0, 1));
        sheet.addMergedRegion(new CellRangeAddress(summaryFirstRow + 2, summaryFirstRow + 2, 0, 1));
        sheet.addMergedRegion(new CellRangeAddress(summaryFirstRow + 3, summaryFirstRow + 3, 0, 1));
        // fill currencies summary
        for (String currency : currencies) {
            final Summary summary = pivot.getCurrencySummary(currency);
            if (CellStyleType.AMOUNT_SHORT == perCurrecyAmountFormat.get(currency)) {
                wb.appendCellWithStyle(total, summary.total, summaryTotalShortStyle);       
            } else {
                wb.appendCellWithStyle(total, summary.total, summaryTotalNormalStyle);
            }
            wb.appendCellWithStyle(payout, summary.totalPayout, summaryStyle);
            wb.appendCellWithStyle(converted, summary.totalConverted, summaryStyle);
            wb.appendCellWithStyle(spread, summary.totalConverted.subtract(summary.totalPayout), summaryStyle);
        }
        
        // total montly summary 
        final CellStyle summaryResultTotalStyle = wb.cloneCellStyle(CellStyleType.AMOUNT);
        final CellStyle summaryResultStyle = wb.cloneCellStyle(CellStyleType.AMOUNT);
        summaryResultTotalStyle.setBorderTop(CellStyle.BORDER_MEDIUM);
        summaryResultTotalStyle.setBorderLeft(CellStyle.BORDER_MEDIUM);        
        summaryResultStyle.setBorderLeft(CellStyle.BORDER_MEDIUM);
        
        final Summary summary = pivot.getSummary();
        wb.appendCellWithStyle(total, "", summaryResultTotalStyle);
        wb.appendCellWithStyle(payout, summary.totalPayout, summaryResultStyle);
        wb.appendCellWithStyle(converted, summary.totalConverted, summaryResultStyle);
        wb.appendCellWithStyle(spread, summary.totalConverted.subtract(summary.totalPayout), summaryResultStyle);
        
        for (int i = 2; i < 2 + currencies.length + 1; i++) {
            sheet.autoSizeColumn(i);            
        }
    }    
    
    private static void createVatSheet(WorkbookStyle wb, List<Transaction> transactions) {
        Log.v("Creating VAT sheet");
        final Sheet sheet = wb.getWorkbook().createSheet("VAT");
        final TreeMap<String, BigDecimal[]> vatCollected = new TreeMap<>();        
        
        for (Transaction t : transactions) {
            if (Transaction.Type.CHARGE == t.getTransactionType() && EU_CURRENCIES.contains(t.getBuyerCurrency())) {
                BigDecimal[] vat = vatCollected.get(t.getBuyerCurrency());
                if (null == vat) {
                    vat = new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
                    vatCollected.put(t.getBuyerCurrency(), vat);
                }
                vat[0] = vat[0].add(t.getTaxAmount());
                vat[1] = vat[1].add(t.getTaxAmountConverted());
            }
        }
        
        final CellStyle centeredCellStyle = wb.cloneCellStyle(CellStyleType.BASE);
        centeredCellStyle.setAlignment(CellStyle.ALIGN_CENTER);
        
        int rowNbr = 0;
        Row row;
        BigDecimal vatTotal = BigDecimal.ZERO;
        
        row = sheet.createRow(rowNbr++);
        wb.appendCellWithStyle(row, "Waluta", CellStyleType.HEADER);
        wb.appendCellWithStyle(row, "VAT", CellStyleType.HEADER);
        wb.appendCellWithStyle(row, "VAT(PLN)", CellStyleType.HEADER);
        for (Map.Entry<String, BigDecimal[]> e : vatCollected.entrySet()) {
            row = sheet.createRow(rowNbr++);
            wb.appendCellWithStyle(row, e.getKey(), centeredCellStyle);
            wb.appendCellWithStyle(row, e.getValue()[0], perCurrecyAmountFormat.get(e.getKey()));
            wb.appendCellWithStyle(row, e.getValue()[1], CellStyleType.AMOUNT);
            vatTotal = vatTotal.add(e.getValue()[1]);
        }
        final CellStyle emptyWithBorder = wb.cloneCellStyle(CellStyleType.BASE);
        final CellStyle amountWithBorder = wb.cloneCellStyle(CellStyleType.AMOUNT);
        emptyWithBorder.setBorderTop(CellStyle.BORDER_MEDIUM);
        amountWithBorder.setBorderTop(CellStyle.BORDER_MEDIUM);
        
        row = sheet.createRow(rowNbr);
        wb.appendCellWithStyle(row, "Łączny VAT(PLN)", emptyWithBorder);
        wb.appendCellWithStyle(row, "", emptyWithBorder);
        sheet.addMergedRegion(new CellRangeAddress(rowNbr + 0, rowNbr + 0, 0, 1));
        wb.appendCellWithStyle(row, vatTotal, amountWithBorder);
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(3);
    }    
    
    private static void createSummarySheet(WorkbookStyle wb, List<Transaction> transactions, boolean noVat) {
        Log.v("Creating summary sheet");
        final Sheet sheet = wb.getWorkbook().createSheet("Podsumowanie");
        BigDecimal totalPayout = BigDecimal.ZERO;
        BigDecimal totalPayoutFromEU = BigDecimal.ZERO;
        BigDecimal totalConverted = BigDecimal.ZERO;
        BigDecimal totalTaxDeduction = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        
        for (Transaction t : transactions) {
            totalPayout = totalPayout.add(t.getPayout());
            if (null == t.getId()) {
                totalTaxDeduction = totalTaxDeduction.add(t.getPayout());
            } else {                
                if (EU_CURRENCIES.contains(t.getBuyerCurrency())) {
                    totalPayoutFromEU = totalPayoutFromEU.add(t.getPayout());
                    if (!noVat) {
                        if (Transaction.Type.CHARGE == t.getTransactionType()) {
                            totalVat = totalVat.add(t.getTaxAmountConverted());
                        }
                    }
                }
                totalConverted = totalConverted.add(t.getAmountConverted());
            }
        }
        
        final CellStyle labelStyle = wb.cloneCellStyle(CellStyleType.HEADER);
        final CellStyle amountStyle = wb.cloneCellStyle(CellStyleType.AMOUNT);
        labelStyle.setAlignment(CellStyle.ALIGN_LEFT);
        labelStyle.setFont(wb.getFont(FontType.NORMAL));
        amountStyle.setFont(wb.getFont(FontType.BOLD));
        
        int rowNbr = 0;
        Row row;
        
        row = sheet.createRow(rowNbr++);
        wb.appendCellWithStyle(row, "Łączny dochód", labelStyle);
        wb.appendCellWithStyle(row, totalPayout, amountStyle);
        row = sheet.createRow(rowNbr++);
        wb.appendCellWithStyle(row, "  w tym z krajów EU", labelStyle);
        wb.appendCellWithStyle(row, totalPayoutFromEU, amountStyle);
        if (!noVat) {
            row = sheet.createRow(rowNbr++);
            wb.appendCellWithStyle(row, "  VAT", labelStyle);
            wb.appendCellWithStyle(row, totalVat, amountStyle);
        }
        row = sheet.createRow(rowNbr++);
        wb.appendCellWithStyle(row, "Łączna różnica kursowa", labelStyle);
        wb.appendCellWithStyle(row, totalConverted.subtract(totalPayout), amountStyle);
        row = sheet.createRow(rowNbr++);
        wb.appendCellWithStyle(row, "Miedzynarodowe podatki", labelStyle);
        wb.appendCellWithStyle(row, totalTaxDeduction.negate(), amountStyle);
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }    
        
    // per currency precision lookup
    private static HashMap<String, CellStyleType> perCurrecyAmountFormat = new HashMap<>();
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMM");
    private static final HashSet<String> EU_CURRENCIES = new HashSet<>(Arrays.asList(
            "EUR", "GBP", "HUF", "HRK", "DKK", "SEK", "BGN", "CZK", "RON", "PLN"));
    
    private static final String CFG_TEMPLATE = 
            "\n\n\n" +
            "# Configuration file template (using standard java .properties file format), by\n" + 
            "# default config file named isp.properties is loaded from current directory if\n" + 
            "# present. \n" + 
            "# Location of configuration file may be override with --config switch.\n" + 
            "\n" + 
            "# GCS related properties, note that they are ignored in local mode\n" + 
            "\n" + 
            "# Service account access configuration, first property defines path to PK12\n" + 
            "# cert file for service account and second one defines e-mail address for \n" + 
            "# service account (see here for instruction how to generate certificate file:\n" + 
            "# https://developers.google.com/console/help/new/#generatingoauth2).\n" + 
            "# Note that both properties has to be set in order to connect to GCS and\n" + 
            "# moreover service e-mail account has to be authorized in Play Developer \n" + 
            "# console for financial data access\n" + 
            "gcs.service.cert.pk12.path = <file path>\n" + 
            "gcs.service.email = <service e-mail>\n" + 
            "\n" + 
            "# Client oauth2 credentials file - access on behalf of user, this will require\n" + 
            "# manual authorization upon first call or when token expires (see here for \n" + 
            "# instruction how to generate certificate file: \n" + 
            "# https://developers.google.com/console/help/new/#generatingoauth2).\n" + 
            "gcs.client.secret.json.path = <file path>\n" + 
            "\n" + 
            "# GCS bucket with reports (as defined in Finances Report page of Play console,\n" + 
            "# see direct report URI in format gs://<bucket>/sales/)\n" + 
            "gcs.bucket = <bucket id>\n" + 
            "\n" + 
            "\n" + 
            "# Basic XLSX style properties\n" + 
            "\n" + 
            "# Font family name and size to use when creating XLSX (default Arial/8)\n" + 
            "style.font.family = <file path>\n" + 
            "style.font.size = <size>\n" + 
            "\n" + 
            "# Headers cell color (default light grey)\n" + 
            "style.header.background.color = <#RRGGBB|named color>\n" +
            "\n" +
            "# Borders color\n" +
            "style.border.color = <#RRGGBB|named color>\n" +
            "\n" + 
            "\n" + 
            "# Output generation control\n" + 
            "\n" + 
            "# Determine if transaction VAT should be collected and processed, VAT \n" + 
            "# information are only stored in sales report so when set to true sales \n" + 
            "# reports will be needed to create final output (important note for local mode:\n" + 
            "# sales report for report month and next one is needed due to differences in\n" + 
            "# billing and charging time).\n" + 
            "# Setting this property to false implies that VAT sheet will not be generated.\n" + 
            "process.transactions.vat = <true|false>\n" + 
            "\n" + 
            "# XLSX sheets generation control (not that xchange sheet need online NBP data\n" + 
            "# so when set to true internet connection is required in order to generate \n" + 
            "# output).\n" + 
            "output.xchange.sheet = <true|false>\n" + 
            "output.vat.sheet = <true|false>\n" + 
            "output.summary.sheet = <true|false>\n" + 
            "\n";
}
