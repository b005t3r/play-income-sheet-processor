package com.bytestorm.isp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Exchange {
    
    public static class Quote {
        
        public Quote() {            
        }
        
        public Quote(String currency, int amount, BigDecimal rate) {
            setCurrency(currency);
            setAmount(amount);
            setRate(rate);
        }                
        
        public String getCurrency() {
            return currency;
        }
        
        public void setCurrency(String currency) {
            this.currency = currency;
        }
        
        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public BigDecimal getRate() {
            return rate;
        }

        public void setRate(BigDecimal rate) {
            this.rate = rate;
        }

        private String currency;
        private int amount;
        private BigDecimal rate;
    }


    @SuppressWarnings("unchecked")
    public Exchange(Date from, Date to) {
        this.from = new DateTime(from, DateTimeZone.UTC);
        this.to = new DateTime(to, DateTimeZone.UTC);        
        this.xchange = new HashMap[Days.daysBetween(this.from, this.to).getDays() + 1];
        Log.v("Creating exchange with date range " + this.from + " - " + this.to + " (days: " + this.xchange.length + ")");
        try {
            this.xmlParser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch(Exception ex) {
            throw new RuntimeException("Cannot initialize XML parser", ex);
        }
        this.decimalParser = (DecimalFormat) NumberFormat.getInstance(new Locale("pl", "PL"));
        this.decimalParser.setParseBigDecimal(true);        
    }
    
    public void download() throws IOException, SAXException, ParseException {
        final URL url = new URL("http://www.nbp.pl/kursy/xml/dir.txt");               
        final LinkedList<DirEntry> tableA = new LinkedList<>();
        final LinkedList<DirEntry> tableB = new LinkedList<>();
        Log.v("Downloading and parsing exchange tables directory");
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            while (null != (line = reader.readLine())) {
                if (line.isEmpty()) {
                    continue;
                }
                char tableType = line.charAt(0);                
                if ('a' == tableType || 'b' == tableType) {
                    final LinkedList<DirEntry> table = ('a' == tableType) ? tableA : tableB;
                    final String dateStr = line.substring(line.indexOf('z') + 1);
                    final DateTime date = dateParser.parseDateTime(dateStr);
                    if (date.isBefore(from)) {
                        if (!table.isEmpty()) {
                            table.removeFirst();
                        }
                        table.add(new DirEntry(tableType, date, line));
                    } else {
                        if (date.isBefore(to)) {
                            table.add(new DirEntry(tableType, date, line));
                        }
                    }
                }                
            }
        }
        Log.v("Directory parsed, relevant A tables counts: " + tableA.size() + " B tables count: " + tableB.size());
        HashMap<String, Quote> current = new HashMap<>();
        // initial table
        parseTable(tableA.getFirst().id, current);
        tableA.removeFirst();
        parseTable(tableB.getFirst().id, current);
        tableB.removeFirst();        
        xchange[0] = current;
        for (DateTime date = from.plusDays(1); !date.isAfter(to); date = date.plusDays(1)) {
            if ((!tableA.isEmpty() && date.isAfter(tableA.getFirst().date)) || 
                    (!tableB.isEmpty() && date.isAfter(tableB.getFirst().date))) {
                current = new HashMap<>(current);
                if (!tableA.isEmpty() && date.isAfter(tableA.getFirst().date)) {                    
                    parseTable(tableA.getFirst().id, current);
                    tableA.removeFirst();
                }
                if (!tableB.isEmpty() && date.isAfter(tableB.getFirst().date)) {
                    parseTable(tableB.getFirst().id, current);
                    tableB.removeFirst();
                }
            }
            int day = Days.daysBetween(from, date).getDays();
            xchange[day] = current;
        }
        Log.v("Exchange data downloaded and processed successfully");
    }
    
    public Quote getQuote(Date date, String currency) {
        final DateTime at = new DateTime(date);
        final Quote entry = xchange[Days.daysBetween(from, at).getDays()].get(currency.toUpperCase(Locale.US));
        if (null == entry) {
            throw new RuntimeException("Entry for currency " + currency + " at " + date + " not found");
        }
        return new Quote(entry.getCurrency(), entry.getAmount(), entry.getRate());
    }
    
    private void parseTable(String id, HashMap<String, Quote> table) throws IOException, SAXException, ParseException {
        Log.v("Downloading and parsing exchange table " + id);
        URL url = new URL("http://www.nbp.pl/kursy/xml/" + id + ".xml");
        try (InputStream is = url.openStream()) {
            final Document doc = xmlParser.parse(new InputSource(is));
            final NodeList nodes = doc.getDocumentElement().getElementsByTagName("pozycja"); 
            for (int i = 0, count = nodes.getLength(); i < count; i++) {
                final Element elem = (Element) nodes.item(i);
                final Quote entry = new Quote();
                for (Node n = elem.getFirstChild(); null != n; n = n.getNextSibling()) {
                    if (n instanceof Element) {
                        Element e = (Element) n;
                        switch (e.getTagName()) {
                        case "przelicznik":
                            entry.setAmount(Integer.parseInt(e.getTextContent()));
                            break;
                        case "kurs_sredni":
                            entry.setRate((BigDecimal) decimalParser.parse(e.getTextContent()));
                            break;
                        case "kod_waluty":
                            entry.setCurrency(e.getTextContent().trim().toUpperCase(Locale.US));
                            break;                            
                        }
                    }
                }
                table.put(entry.getCurrency(), entry);
            }
        }
    }
        
    static class DirEntry {
        
        public DirEntry() {            
        }
        
        public DirEntry(char tableType, DateTime date, String id) {
            setTableType(tableType);
            setDate(date);
            setId(id);
        }

        public char getTableType() {
            return tableType;
        }

        public void setTableType(char tableType) {
            this.tableType = tableType;
        }

        public DateTime getDate() {
            return date;
        }

        public void setDate(DateTime date) {
            this.date = date;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
        private char tableType;
        private DateTime date;
        private String id;
    }
    
    private DateTime from, to;    
    private HashMap<String, Quote>[] xchange;
    private DocumentBuilder xmlParser;
    private DecimalFormat decimalParser;
    private DateTimeFormatter dateParser = DateTimeFormat.forPattern("yyMMdd").withZoneUTC();
}
