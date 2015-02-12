package com.bytestorm.isp;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalReports implements ReportsProvider {

    public LocalReports(File dir) throws IOException, IllegalArgumentException {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Local reports path " + dir.getAbsolutePath() + " is not directory");
        }
        final File[] reports = dir.listFiles();
        if (null == reports || reports.length < 1) {
            throw new IllegalArgumentException("Local reports director " + dir.getAbsolutePath() + " is empty");
        }                        
        // parse content of provided directory                
        for (File f : reports) {
            final String filename = f.getName();
            Matcher m;
            m = RE_EARNINGS_ZIP.matcher(filename);
            if (m.matches()) {
                date = checkDates(date, m.group(1));
                final File earningReportCsv = File.createTempFile("earnings", ".csv");
                Utils.unpack(f, earningReportCsv);
                earningsReports.add(earningReportCsv);
                continue;
            }
            m = RE_SALES_ZIP.matcher(filename); 
            if (m.matches()) {
                final File salesReportCsv = File.createTempFile("sales", ".csv");
                Utils.unpack(f, salesReportCsv);
                salesReports.add(salesReportCsv);
                continue;
            }
            m = RE_CVS.matcher(filename);
            if (m.matches()) {                    
                final String type = m.group(1);
                switch (type.toLowerCase()) {
                case "playapps":
                case "earnings":
                    date = checkDates(date, m.group(2));
                    earningsReports.add(f);
                    break;
                case "salesreport":
                case "sales":              
                    salesReports.add(f);
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized CSV file " + filename);
                }
                continue;
            }
            throw new IllegalArgumentException("Unrecognize report file " + filename);
        }
        if (earningsReports.isEmpty()) {
            throw new IllegalArgumentException("Earnings reports not found in directory " + dir.getAbsolutePath());        
        }
        try {
            reportsDate = DATE_FORMAT.parse(date);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Cannot parse date extraceted from report files " + date);
        }
    }

    @Override
    public File[] getEarningsReportsFiles() throws IOException, IllegalArgumentException {
        return earningsReports.toArray(new File[earningsReports.size()]);
    }

    @Override
    public File[] getSalesReportsFiles() throws IOException, IllegalArgumentException {
        return salesReports.toArray(new File[salesReports.size()]);
    }
    
    @Override
    public Date getDate() {
        return reportsDate;
    }    

    private static String checkDates(String date1, String date2) throws IllegalArgumentException {
        if (null == date2) {
            throw new IllegalArgumentException("Date not matched in report filename");
        }
        if (null == date1) {            
            return date2;
        }
        if (!date1.equals(date2)) {
            System.err.println("Different reports date " + date1 + " and " + date2);
        }
        return date1;
    }

    private String date;
    private Date reportsDate;
    private ArrayList<File> earningsReports = new ArrayList<>();
    private ArrayList<File> salesReports = new ArrayList<>();
    
    // file names regexps
    private static final Pattern RE_SALES_ZIP = Pattern.compile("^salesreport_(\\d{6})\\.zip$");
    private static final Pattern RE_EARNINGS_ZIP = Pattern.compile("^earnings_(\\d{6})_\\d{16}-\\d+\\.zip$");
    private static final Pattern RE_CVS = Pattern.compile("^(\\p{Alpha}*)_(\\d{6})\\.zip$");
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMM");
}
