package com.bytestorm.isp;

import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.Locale;

import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseBigDecimal;
import org.supercsv.cellprocessor.ParseLong;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;

public class Sale {
    
    // City of Buyer,State of Buyer,Postal Code of Buyer,Country of Buyer
    public static final String MAPPING[] = new String[] {
        "id",
        null,
        "timestamp", // unix epoch
        null,
        null,
        null,
        null,
        null,
        null,
        "buyerCurrency",
        "price",
        "taxCollected",
        "chargedAmount",
        null,
        null,
        null,
        "buyerCountry"
    };    
    
    public static final CellProcessor CSV[] = new CellProcessor[] {
        new NotNull(),
        null,
        new ParseLong(),
        null,
        null,
        null,
        null,
        null,
        null,
        new NotNull(),
        new ParseBigDecimal(DecimalFormatSymbols.getInstance(Locale.US)),
        new ParseBigDecimal(DecimalFormatSymbols.getInstance(Locale.US)),
        new ParseBigDecimal(DecimalFormatSymbols.getInstance(Locale.US)),
        null,
        null,
        null,
        new Optional()
    };

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public long getTimestamp() {
        return timestamp; // unix epoch
    }
    
    public Date getTimestmpDate() {
        return new Date(timestamp * 1000L);
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getBuyerCurrency() {
        return buyerCurrency;
    }

    public void setBuyerCurrency(String buyerCurrency) {
        this.buyerCurrency = buyerCurrency;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTaxCollected() {
        return taxCollected;
    }

    public void setTaxCollected(BigDecimal taxCollected) {
        this.taxCollected = taxCollected;
    }

    public BigDecimal getChargedAmount() {
        return chargedAmount;
    }

    public void setChargedAmount(BigDecimal chargedAmount) {
        this.chargedAmount = chargedAmount;
    }
    
    public String getBuyerCountry() {
        return buyerCountry;
    }

    public void setBuyerCountry(String buyerCountry) {
        this.buyerCountry = buyerCountry;
    }    
    
    private String id;
    private long timestamp;
    private String buyerCurrency;
    private BigDecimal price; // netto price
    private BigDecimal taxCollected;
    private BigDecimal chargedAmount; // brutto price (tax + netto)
    private String buyerCountry;
}

