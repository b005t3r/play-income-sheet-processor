package com.bytestorm.isp;

import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.supercsv.cellprocessor.HashMapper;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseBigDecimal;
import org.supercsv.cellprocessor.ParseDate;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;

/**
 * Transaction bean.
 */
public class Transaction {
    public static enum Type {
        CHARGE("Charge"), FEE("Google fee"), REFUND("Charge refund"), FEE_REFUND("Google fee refund"), TAX("Tax");
        
        private Type(String desc) {
            this.desc = desc;
        }
        
        public String toString() {
            return desc;
        }
        
        private String desc;
        
        public static Map<Object, Object> MAP = new HashMap<>();
        static {
            for (Type t : values()) {
                MAP.put(t.toString(), t);
            }
        }
    }
    
    // input CSV structure
    public static final String MAPPING[] = new String[] {
            "id",
            "date",
            "time",
            "taxType",
            "transactionType",
            "refundType",
            "productName",
            "applicationId",
            "productType",
            "skuId",
            "hardware",
            "buyerCountry",
            "buyerState",
            "buyerPostalCode",
            "buyerCurrency",
            "amount",
            "conversionRate",
            "merchantCurrency",
            "payout",
    };
    public static final CellProcessor CSV[] = new CellProcessor[] {
            new Optional(),
            new ParseDateEx("MMM d, yyyy", true, Locale.US, TimeZone.getTimeZone("UTC")),
            new ParseDate("h:mm:ss a", true, Locale.US),
            new Optional(),
            new Optional(new HashMapper(Type.MAP)),
            new Optional(),
            new Optional(),
            new Optional(),
            new Optional(new ParseInt()),
            new Optional(),
            new Optional(),
            new Optional(),
            new Optional(),
            new Optional(),
            new NotNull(),
            new ParseBigDecimal(DecimalFormatSymbols.getInstance(Locale.US)),
            new ParseBigDecimal(DecimalFormatSymbols.getInstance(Locale.US)),
            new NotNull(),
            new ParseBigDecimal(DecimalFormatSymbols.getInstance(Locale.US)),
    };

    public Transaction() {
    }

    /**
     * Unique transaction id getter.
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Unique transaction id setter.
     * @param id
     *            the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Transaction data getter.
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * @param date
     *            the date to set
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * @return the time
     */
    public Date getTime() {
        return time;
    }

    /**
     * @param time
     *            the time to set
     */
    public void setTime(Date time) {
        this.time = time;
    }

    /**
     * @return the tax type
     */
    public String getTaxType() {
        return taxType;
    }

    /**
     * @param taxType
     *            the tax type to set
     */
    public void setTaxType(String taxType) {
        this.taxType = taxType;
    }

    /**
     * @return the transactionType
     */
    public Type getTransactionType() {
        return transactionType;
    }

    /**
     * @param transactionType
     *            the transactionType to set
     */
    public void setTransactionType(Type transactionType) {
        this.transactionType = transactionType;
    }

    /**
     * @return the refundType
     */
    public String getRefundType() {
        return refundType;
    }

    /**
     * @param refundType
     *            the refundType to set
     */
    public void setRefundType(String refundType) {
        this.refundType = refundType;
    }

    /**
     * @return the productName
     */
    public String getProductName() {
        return productName;
    }

    /**
     * @param productName
     *            the productName to set
     */
    public void setProductName(String productName) {
        this.productName = productName;
    }

    /**
     * @return the applicationId
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * @param applicationId
     *            the applicationId to set
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * @return the productType
     */
    public int getProductType() {
        return productType;
    }

    /**
     * @param productType
     *            the productType to set
     */
    public void setProductType(int productType) {
        this.productType = productType;
    }

    /**
     * @return the skuId
     */
    public String getSkuId() {
        return skuId;
    }

    /**
     * @param skuId
     *            the skuId to set
     */
    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    /**
     * @return the hardware
     */
    public String getHardware() {
        return hardware;
    }

    /**
     * @param hardware
     *            the hardware to set
     */
    public void setHardware(String hardware) {
        this.hardware = hardware;
    }

    /**
     * @return the buyerCountry
     */
    public String getBuyerCountry() {
        return buyerCountry;
    }

    /**
     * @param buyerCountry
     *            the buyerCountry to set
     */
    public void setBuyerCountry(String buyerCountry) {
        this.buyerCountry = buyerCountry;
    }

    /**
     * @return the buyerState
     */
    public String getBuyerState() {
        return buyerState;
    }

    /**
     * @param buyerState
     *            the buyerState to set
     */
    public void setBuyerState(String buyerState) {
        this.buyerState = buyerState;
    }

    /**
     * @return the buyerPostalCode
     */
    public String getBuyerPostalCode() {
        return buyerPostalCode;
    }

    /**
     * @param buyerPostalCode
     *            the buyerPostalCode to set
     */
    public void setBuyerPostalCode(String buyerPostalCode) {
        this.buyerPostalCode = buyerPostalCode;
    }

    /**
     * @return the buyerCurrency
     */
    public String getBuyerCurrency() {
        return buyerCurrency;
    }

    /**
     * @param buyerCurrency
     *            the buyerCurrency to set
     */
    public void setBuyerCurrency(String buyerCurrency) {
        this.buyerCurrency = buyerCurrency;
    }

    /**
     * @return the amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * @param amount
     *            the amount to set
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * @return the conversionRate
     */
    public BigDecimal getConversionRate() {
        return conversionRate;
    }

    /**
     * @param conversionRate
     *            the conversionRate to set
     */
    public void setConversionRate(BigDecimal conversionRate) {
        this.conversionRate = conversionRate;
    }

    /**
     * @return the merchantCurrency
     */
    public String getMerchantCurrency() {
        return merchantCurrency;
    }

    /**
     * @param merchantCurrency
     *            the merchantCurrency to set
     */
    public void setMerchantCurrency(String merchantCurrency) {
        this.merchantCurrency = merchantCurrency;
    }

    /**
     * @return the payout
     */
    public BigDecimal getPayout() {
        return payout;
    }

    /**
     * @param payout
     *            the payout to set
     */
    public void setPayout(BigDecimal payout) {
        this.payout = payout;
    }
    
    /**
     * 
     * @return
     */
    public int getConversionRateBaseAmount() {
        return conversionRateBaseAmount;
    }
    
    /**
     * 
     * @param conversionRateBaseAmount
     */
    public void setConversionRateBaseAmount(int conversionRateBaseAmount) {
        this.conversionRateBaseAmount = conversionRateBaseAmount;
    }

    /**
     * @return the amountConverted
     */
    public BigDecimal getAmountConverted() {
        if (buyerCurrency.equals(merchantCurrency)) {
            return amount;
        }
        return amount.multiply(conversionRate).divide(new BigDecimal(conversionRateBaseAmount));
    }

    /**
     * @param amountConverted
     *            the amountConverted to set
     */
    public BigDecimal getSpread() {
        if (buyerCurrency.equals(merchantCurrency)) {
            return BigDecimal.ZERO;
        }
        return payout.subtract(getAmountConverted());
    }
    
    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }
    
    public BigDecimal getTaxAmount() {
        return taxAmount;
    }
    
    public BigDecimal getTaxAmountConverted() {
        if (buyerCurrency.equals(merchantCurrency)) {
            return taxAmount;
        }        
        return taxAmount.multiply(conversionRate).divide(new BigDecimal(conversionRateBaseAmount));
    }

    private String id;
    private Date date;
    private Date time;
    private String taxType;
    private Type transactionType;
    private String refundType;
    private String productName;
    private String applicationId;
    private int productType;
    private String skuId;
    private String hardware;
    private String buyerCountry;
    private String buyerState;
    private String buyerPostalCode;
    private String buyerCurrency;
    private BigDecimal amount;
    private BigDecimal conversionRate;
    private String merchantCurrency;
    private BigDecimal payout;
    private int conversionRateBaseAmount;    
    private BigDecimal taxAmount;    
}
