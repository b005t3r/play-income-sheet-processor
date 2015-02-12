package com.bytestorm.isp;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Properties;

public class Configuration extends Properties {
    private static final long serialVersionUID = 1L;

    public Configuration() {
    }

    /**
     * Get the value of the property with the given name, return the value as an
     * <code>int</code>.
     * 
     * @param propertyName
     *            The property name.
     * @param defaultValue
     *            The value to return if a property with the given name does not
     *            exist or is not an integer.
     *
     * @return The value.
     */
    public final int getInt(String propertyName, int defaultValue) {
        final String s = getProperty(propertyName);
        if (s != null) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return defaultValue;
    }

    /**
     * Set the property with the given name to an <code>int</code> value.
     * 
     * @param propertyName
     *            The property name.
     * @param value
     *            The value to set.
     */
    public final void setInt(String propertyName, int value) {
        setProperty(propertyName, Integer.toString(value));
    }

    /**
     * Get the value of the property with the given name, return the value as a
     * <code>long</code>.
     * 
     * @param propertyName
     *            The property name.
     * @param defaultValue
     *            The value to return if a property with the given name does not
     *            exist or is not a long.
     *
     * @return The value.
     */
    public final long getLong(String propertyName, long defaultValue) {
        final String s = getProperty(propertyName);
        if (s != null) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return defaultValue;
    }

    /**
     * Set the property with the given name to a <code>long</code> value.
     * 
     * @param propertyName
     *            The property name.
     * @param value
     *            The value to set.
     */
    public final void setLong(String propertyName, long value) {
        setProperty(propertyName, Long.toString(value));
    }

    /**
     * Get the value of the property with the given name, return the value as a
     * <code>short</code>.
     * 
     * @param propertyName
     *            The property name.
     * @param defaultValue
     *            The value to return if a property with the given name does not
     *            exist or is not a short.
     *
     * @return The value.
     */
    public final short getShort(String propertyName, short defaultValue) {
        final String s = getProperty(propertyName);
        if (s != null) {
            try {
                return Short.parseShort(s.trim());
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return defaultValue;
    }

    /**
     * Set the property with the given name to a <code>short</code> value.
     * 
     * @param propertyName
     *            The property name.
     * @param value
     *            The value to set.
     */
    public final void setShort(String propertyName, short value) {
        setProperty(propertyName, Short.toString(value));
    }

    /**
     * Get the value of the property with the given name, return the value as a
     * <code>double</code>.
     * 
     * @param propertyName
     *            The property name.
     * @param defaultValue
     *            The value to return if a property with the given name does not
     *            exist or is not a double.
     *
     * @return The value.
     */
    public final double getDouble(String propertyName, double defaultValue) {
        final String s = getProperty(propertyName);
        if (s != null) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return defaultValue;
    }

    /**
     * Set the property with the given name to a <code>double</code> value.
     * 
     * @param propertyName
     *            The property name.
     * @param value
     *            The value to set.
     */
    public final void setDouble(String propertyName, double value) {
        setProperty(propertyName, Double.toString(value));
    }

    /**
     * Get the value of the property with the given name, return the value as a
     * <code>boolean</code>.
     * 
     * @param propertyName
     *            The property name.
     * @param defaultValue
     *            The value to return if a property with the given name does not
     *            exist.
     *
     * @return The value.
     */
    public final boolean getBoolean(String propertyName, boolean defaultValue) {
        final String s = getProperty(propertyName);
        if (s != null) {
            return Boolean.valueOf(s).booleanValue();
        }
        return defaultValue;
    }

    /**
     * Set the property with the given name to a <code>boolean</code> value.
     * 
     * @param propertyName
     *            The property name.
     * @param value
     *            The value to set.
     */
    public final void setBoolean(String propertyName, boolean value) {
        setProperty(propertyName, String.valueOf(value));
    }
    
    /**
     * Get the value of the property with the given name, return the value as a
     * <code>java.awt.Color</code>.
     * 
     * @param propertyName
     *            The property name.
     * @param defaultValue
     *            The value to return if a property with the given name does not
     *            exist or cannot be converted to color.
     *
     * @return The value.
     */    
    public final Color getColor(String propertyName, Color defaultValue) {
        String s = getProperty(propertyName);
        if (s != null) {
            s = s.trim();
            if (s.charAt(0) == '#') {
                final int len = s.length() - 1;
                try {
                    switch (len) {
                    case 3:
                        return new Color(
                                parseHex2(s.substring(1, 2)), 
                                parseHex2(s.substring(2, 3)), 
                                parseHex2(s.substring(3)));
                    case 4:
                        return new Color(
                                parseHex2(s.substring(2, 3)), 
                                parseHex2(s.substring(3, 4)), 
                                parseHex2(s.substring(4)), 
                                parseHex2(s.substring(1, 2)));
                    case 6:
                        return new Color(
                                parseHex(s.substring(1, 3)), 
                                parseHex(s.substring(3, 5)), 
                                parseHex(s.substring(5)));
                    case 8:                    
                        return new Color(
                                parseHex(s.substring(3, 5)), 
                                parseHex(s.substring(5, 7)), 
                                parseHex(s.substring(7)),
                                parseHex(s.substring(1, 3)));
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
                return defaultValue;
            } else {
                try {
                    Field f = Color.class.getField(s);
                    return (Color) f.get(null);
                } catch (Throwable ex) {
                    // ignore
                }
            }
        }
        return defaultValue;
    }
    
    private int parseHex2(String c) { 
        return parseHex(c + c);
    }
    
    private int parseHex(String n) {
        return Integer.parseInt(n, 16);
    }
}