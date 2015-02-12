package com.bytestorm.isp;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder.BorderSide;

public class WorkbookStyle {
    public static final String CONFIG_FONT_FAMILY = "style.font.family";
    public static final String CONFIG_FONT_SIZE = "style.font.size";
    public static final String CONFIG_HEADER_BACKGROUND_COLOR = "style.header.background.color";
    public static final String CONFIG_BORDER_COLOR = "style.border.color";
    
    public enum FontType { NORMAL, BOLD, ITALIC }
    public enum CellStyleType { BASE, HEADER, DATE, AMOUNT, AMOUNT_SHORT, EMPTY }

    public WorkbookStyle(Workbook wb, Configuration cfg) {
        this.wb = wb;
        this.dataFormat = wb.createDataFormat();
        this.fonts = new Font[FontType.values().length];
        this.cellStyles = new CellStyle[CellStyleType.values().length];
        initFonts(cfg);
        initCellStyles(cfg);
    }
    
    public Font getFont(FontType fontType) {
        return fonts[fontType.ordinal()];
    }
    
    public CellStyle getCellStyle(CellStyleType cellStyleType) {
        return cellStyles[cellStyleType.ordinal()];
    }
    
    public CellStyle cloneCellStyle(CellStyleType cellStyleType) {
        CellStyle style = wb.createCellStyle();
        style.cloneStyleFrom(getCellStyle(cellStyleType));
        return style;
    }
    
    public short getFormat(String formatString) {
        return dataFormat.getFormat(formatString);
    }
    
    public void addCellWithStyle(Row row, int n, Date date, CellStyle fmt) {
        Cell cell = row.createCell(n); 
        cell.setCellValue(date);
        cell.setCellStyle(fmt);
    }
    
    public void addCellWithStyle(Row row, int n, BigDecimal number, CellStyle fmt) {
        Cell cell = row.createCell(n); 
        cell.setCellValue(number.doubleValue());
        cell.setCellStyle(fmt);
    }
    
    public void addCellWithStyle(Row row, int n, String value, CellStyle fmt) {
        Cell cell = row.createCell(n, Cell.CELL_TYPE_STRING); 
        cell.setCellValue(value);
        cell.setCellStyle(fmt);
    }
    
    public void addCellWithStyle(Row row, int n, Date date, CellStyleType fmt) {
        addCellWithStyle(row, n, date, getCellStyle(fmt));
    }
    
    public void addCellWithStyle(Row row, int n, BigDecimal number, CellStyleType fmt) {
        addCellWithStyle(row, n, number, getCellStyle(fmt));
    }
    
    public void addCellWithStyle(Row row, int n, String value, CellStyleType fmt) {
        addCellWithStyle(row, n, value, getCellStyle(fmt));
    }
    
    public void appendCellWithStyle(Row row, Date date, CellStyle fmt) {
        int lastCellNdx = row.getLastCellNum();
        addCellWithStyle(row, lastCellNdx < 0 ? 0 : lastCellNdx, date, fmt);
    }
    
    public void appendCellWithStyle(Row row, BigDecimal number, CellStyle fmt) {
        int lastCellNdx = row.getLastCellNum();
        addCellWithStyle(row, lastCellNdx < 0 ? 0 : lastCellNdx, number, fmt);
    }
    
    public void appendCellWithStyle(Row row, String value, CellStyle fmt) {
        int lastCellNdx = row.getLastCellNum();
        addCellWithStyle(row, lastCellNdx < 0 ? 0 : lastCellNdx, value, fmt);
    }
    
    public void appendCellWithStyle(Row row, Date date, CellStyleType fmt) {
        int lastCellNdx = row.getLastCellNum();
        addCellWithStyle(row, lastCellNdx < 0 ? 0 : lastCellNdx, date, fmt);
    }
    
    public void appendCellWithStyle(Row row, BigDecimal number, CellStyleType fmt) {
        int lastCellNdx = row.getLastCellNum();
        addCellWithStyle(row, lastCellNdx < 0 ? 0 : lastCellNdx, number, fmt);
    }
    
    public void appendCellWithStyle(Row row, String value, CellStyleType fmt) {
        int lastCellNdx = row.getLastCellNum();
        addCellWithStyle(row, lastCellNdx < 0 ? 0 : lastCellNdx, value, fmt);
    }

    public Workbook getWorkbook() {
        return wb;
    }
    
    private void initFonts(Configuration cfg) {
        final Font normalFont = wb.createFont();
        final Font boldFont = wb.createFont();
        final Font italicFont = wb.createFont();
        
        normalFont.setFontHeightInPoints(cfg.getShort(CONFIG_FONT_SIZE, DEFAULT_FONT_SIZE));
        normalFont.setFontName(cfg.getProperty(CONFIG_FONT_FAMILY, DEFAULT_FONT_FAMILY));
        
        boldFont.setFontHeightInPoints(cfg.getShort(CONFIG_FONT_SIZE, DEFAULT_FONT_SIZE));
        boldFont.setFontName(cfg.getProperty(CONFIG_FONT_FAMILY, DEFAULT_FONT_FAMILY));
        boldFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        
        italicFont.setFontHeightInPoints(cfg.getShort(CONFIG_FONT_SIZE, DEFAULT_FONT_SIZE));
        italicFont.setFontName(cfg.getProperty(CONFIG_FONT_FAMILY, DEFAULT_FONT_FAMILY));
        italicFont.setItalic(true);
        
        fonts[FontType.NORMAL.ordinal()] = normalFont;
        fonts[FontType.BOLD.ordinal()] = boldFont;
        fonts[FontType.ITALIC.ordinal()] = italicFont;
    }
    
    private void initCellStyles(Configuration cfg) {
        // base style
        final XSSFCellStyle baseStyle = (XSSFCellStyle) wb.createCellStyle();        
        baseStyle.setBorderLeft(CellStyle.BORDER_HAIR);
        baseStyle.setBorderTop(CellStyle.BORDER_HAIR);
        baseStyle.setBorderRight(CellStyle.BORDER_HAIR);
        baseStyle.setBorderBottom(CellStyle.BORDER_HAIR);
        XSSFColor borderColor = new XSSFColor(cfg.getColor(CONFIG_BORDER_COLOR, DEFAULT_BORDER_COLOR));
        for (BorderSide side : BorderSide.values()) {
            baseStyle.setBorderColor(side, borderColor);
        }
        baseStyle.setFont(getFont(FontType.NORMAL));
        
        // header sytle
        final XSSFCellStyle headerStyle = (XSSFCellStyle) wb.createCellStyle();
        headerStyle.cloneStyleFrom(baseStyle);
        headerStyle.setAlignment(CellStyle.ALIGN_CENTER);
        headerStyle.setFont(getFont(FontType.BOLD));
        headerStyle.setFillForegroundColor(new XSSFColor(cfg.getColor(CONFIG_HEADER_BACKGROUND_COLOR, DEFAULT_HEADER_COLOR)));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // data cell format
        final CellStyle dateCellStyle = wb.createCellStyle();
        dateCellStyle.cloneStyleFrom(baseStyle);
        dateCellStyle.setDataFormat(getFormat("dd.MM.yy"));
        dateCellStyle.setAlignment(CellStyle.ALIGN_CENTER);
        
        // amount normal
        final CellStyle amountCellStyle = wb.createCellStyle();
        amountCellStyle.cloneStyleFrom(baseStyle);
        amountCellStyle.setDataFormat(getFormat("# ##0.00;[RED]-# ##0.00"));
        // amount short
        final CellStyle amountShortCellStyle = wb.createCellStyle();
        amountShortCellStyle.cloneStyleFrom(baseStyle);
        amountShortCellStyle.setDataFormat(getFormat("# ##0;[RED]-# ##0"));
        
        cellStyles[CellStyleType.BASE.ordinal()] = baseStyle;
        cellStyles[CellStyleType.HEADER.ordinal()] = headerStyle;
        cellStyles[CellStyleType.DATE.ordinal()] = dateCellStyle;
        cellStyles[CellStyleType.AMOUNT.ordinal()] = amountCellStyle;
        cellStyles[CellStyleType.AMOUNT_SHORT.ordinal()] = amountShortCellStyle;
        cellStyles[CellStyleType.EMPTY.ordinal()] = wb.createCellStyle();
    }
    
    private Workbook wb;
    private DataFormat dataFormat;
    private Font[] fonts;
    private CellStyle[] cellStyles;
    
    private static final String DEFAULT_FONT_FAMILY = "Arial";
    private static final short DEFAULT_FONT_SIZE = 8;
    private static final Color DEFAULT_HEADER_COLOR = new Color(240, 240, 240);
    private static final Color DEFAULT_BORDER_COLOR = Color.BLACK;
}
