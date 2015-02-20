package com.bytestorm.isp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Date to currency pivot report.
 */
public class MonthlyPivotReport {
    
    public static class Summary {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalPayout = BigDecimal.ZERO;
        BigDecimal totalConverted = BigDecimal.ZERO;
        
        private Summary() {            
        }
    }
    
    private static class PivotData {
        Summary summary = new Summary();
        ArrayList<Transaction> transactions = new ArrayList<>();
        
        void add(Transaction transact) {
            transactions.add(transact);
            summary.total = summary.total.add(transact.getAmount());
            summary.totalPayout = summary.totalPayout.add(transact.getPayout());
            summary.totalConverted = summary.totalConverted.add(transact.getAmountConverted());            
        }
    }
    
    private static class SummaryIterable implements Iterable<Summary> {
        
        SummaryIterable(PivotData[] data) {
            this.data = data;
        } 
                
        @Override
        public Iterator<Summary> iterator() {
            return new Iterator<Summary>() {
                @Override
                public boolean hasNext() {
                    return ndx < data.length;
                }

                @Override
                public Summary next() {
                    return data[++ndx].summary;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }  
                
                private int ndx = -1;
            };
        }
        
        private PivotData[] data;
   }

    public MonthlyPivotReport(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("Transaction list is empty");
        }
        // query date (report month/year) and currency (list of used currencies) info 
        queryBaseInfo(transactions);
        Log.v("Creating montly pivot report for " + month + "." + year);
        // initialize data storage            
        perDay = singleDimPivotDataArray(daysCount);
        perCurrency = singleDimPivotDataArray(currencies.length);
        perDayCurrency = new PivotData[daysCount][currencies.length];
        for (int i = 0; i < daysCount; i++) {
            for (int j = 0; j < currencies.length; j++) {
                perDayCurrency[i][j] = new PivotData();
            }
        }
        Log.v("  days in month: " + daysCount + " currencies count: " + currencies.length);
        // construct pivot data
        for (Transaction t : transactions) {
            final int day = new DateTime(t.getDate()).getDayOfMonth() - 1;
            final int currency = currencyIndexes.get(t.getBuyerCurrency());
            perDay[day].add(t);
            perCurrency[currency].add(t);
            perDayCurrency[day][currency].add(t);
        }
    }

    public Date[] getDays() {
        Date[] days = new Date[daysCount];        
        for (int i = 0; i < daysCount; i++) {
            days[i] = new DateTime(year, month, i + 1, 0, 0, DateTimeZone.UTC).toDate();
        }
        return days;
    }
    
    public String[] getCurrencies() {
        return currencies;
    }
    
    public Date getDate(int day) {
        return new DateTime(day, month, year, 0, 0).toDate();
    }
    
    public Summary getDaySummary(Date date) {
        final DateTime dt = new DateTime(date, DateTimeZone.UTC);
        return perDay[dt.getDayOfMonth() - 1].summary;
    }
    
    public Summary getCurrencySummary(String currency) {
        final Integer index = currencyIndexes.get(currency);
        if (null == index) {
            throw new IllegalArgumentException("Invalid currency code");
        }
        return perCurrency[index].summary;
    }
    
    public Summary getDayCurrencySummary(Date date, String currency) {
        final DateTime dt = new DateTime(date, DateTimeZone.UTC);
        final Integer index = currencyIndexes.get(currency);
        if (null == index) {
            throw new IllegalArgumentException("Invalid currency code");
        }
        return perDayCurrency[dt.getDayOfMonth() - 1][index].summary;
    }
    
    public Iterable<Summary> getDaySummaryRow(Date date) {
        final DateTime dt = new DateTime(date, DateTimeZone.UTC);        
        return new SummaryIterable(perDayCurrency[dt.getDayOfMonth() - 1]);
    }
    
    public Summary getSummary() {
        return summary;
    }
    
    private void queryBaseInfo(List<Transaction> transactions) {
        final DateTime firstDate = new DateTime(transactions.get(0).getDate());
        final TreeSet<String> currenciesSet = new TreeSet<>();
        // verify that all transactions are from single month
        for (Transaction t : transactions) {            
            final DateTime date = new DateTime(t.getDate(), DateTimeZone.UTC);
            if (date.getYear() != firstDate.getYear() && date.getMonthOfYear() != firstDate.getMonthOfYear()) {
                throw new IllegalArgumentException("Transactions from more than one month " + date + " " + firstDate);
            }
            currenciesSet.add(t.getBuyerCurrency());
            // summary
            summary.total = summary.total.add(t.getAmount());
            summary.totalPayout = summary.totalPayout.add(t.getPayout());
            summary.totalConverted = summary.totalConverted.add(t.getAmountConverted());
        }
        month = firstDate.getMonthOfYear();
        year = firstDate.getYear();
        daysCount = firstDate.dayOfMonth().getMaximumValue();
        // initialize currencies
        currencies = currenciesSet.toArray(new String[currenciesSet.size()]);
        for (int i = 0; i < currencies.length; i++) {
            currencyIndexes.put(currencies[i], i);
        }
    }
    
    private PivotData[] singleDimPivotDataArray(int size) {
        PivotData out[] = new PivotData[size];
        for (int i = 0; i < size; i++) {
            out[i] = new PivotData();
        }
        return out;
    }
    
    private int month, year, daysCount;
    
    private String[] currencies;
    private HashMap<String, Integer> currencyIndexes = new HashMap<>();
    // data storage
    private PivotData[] perDay;
    private PivotData[] perCurrency;
    private PivotData[][] perDayCurrency;
    private Summary summary = new Summary();
}
