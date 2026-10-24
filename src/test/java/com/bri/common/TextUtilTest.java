package com.bri.common;

import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link TextUtil} 单元测试（不连数据库）。
 *
 * @author A
 */
public class TextUtilTest {

    @Test
    public void isBlankShouldHandleNullAndWhitespace() {
        assertTrue(TextUtil.isBlank(null));
        assertTrue(TextUtil.isBlank(""));
        assertTrue(TextUtil.isBlank("   "));
        assertTrue(TextUtil.isBlank("\t\n"));
        assertTrue(TextUtil.isBlank("\u3000"));
        assertFalse(TextUtil.isBlank(" 项目 "));
        assertTrue(TextUtil.isNotBlank(" 项目 "));
        assertFalse(TextUtil.isNotBlank(null));
    }

    @Test
    public void trimToNullShouldNormalizeEmptyInput() {
        assertNull(TextUtil.trimToNull(null));
        assertNull(TextUtil.trimToNull("   "));
        assertEquals("蒙内铁路", TextUtil.trimToNull("  蒙内铁路 "));
    }

    @Test
    public void defaultValueShouldApplyOnlyWhenBlank() {
        assertEquals("未填写", TextUtil.defaultIfBlank("  ", "未填写"));
        assertEquals("在建", TextUtil.defaultIfBlank("在建", "未填写"));
        assertEquals("", TextUtil.nullToEmpty(null));
        assertEquals("x", TextUtil.nullToEmpty("x"));
        assertEquals("", TextUtil.safe(null));
        assertEquals("12", TextUtil.safe(12));
    }

    @Test
    public void escapeLikeShouldEscapeWildcards() {
        assertEquals("50\\%\\_a\\\\b", TextUtil.escapeLike("50%_a\\b"));
        assertNull(TextUtil.escapeLike(null));
    }

    @Test
    public void containsLikeShouldWrapTrimmedKeyword() {
        assertEquals("%铁路%", TextUtil.containsLike(" 铁路 "));
        assertNull(TextUtil.containsLike("   "));
        assertNull(TextUtil.containsLike(null));
    }

    @Test
    public void likeEscapeClauseShouldBeUsableInSql() {
        assertTrue(TextUtil.LIKE_ESCAPE_SQL.contains("ESCAPE"));
        assertTrue(TextUtil.LIKE_ESCAPE_SQL.contains("\\"));
    }

    @Test
    public void formatAmountShouldUseThousandsSeparator() {
        assertEquals("1,234,567.89", TextUtil.formatAmount(new BigDecimal("1234567.891")));
        assertEquals("0.00", TextUtil.formatAmount(BigDecimal.ZERO));
        assertEquals("", TextUtil.formatAmount(null));
    }

    @Test
    public void formatPercentShouldRoundHalfUp() {
        assertEquals("98.5%", TextUtil.formatPercent(new BigDecimal("98.456"), 1));
        assertEquals("0%", TextUtil.formatPercent(new BigDecimal("0.4"), 0));
        assertEquals("100.00%", TextUtil.formatPercent(100.0d, 2));
        assertEquals("", TextUtil.formatPercent((BigDecimal) null, 2));
        assertEquals("", TextUtil.formatPercent(Double.NaN, 2));
    }

    @Test
    public void toDisplayShouldFormatDatabaseValues() {
        assertEquals("2024-03-01", TextUtil.toDisplay(Date.valueOf("2024-03-01")));
        assertEquals("2024-03-01 08:30:00",
                TextUtil.toDisplay(java.sql.Timestamp.valueOf("2024-03-01 08:30:00")));
        assertEquals("1200.50", TextUtil.toDisplay(new BigDecimal("1200.50")));
        assertEquals("12", TextUtil.toDisplay(12));
        assertEquals("", TextUtil.toDisplay(null));
    }

    @Test
    public void truncateShouldAppendEllipsis() {
        assertEquals("abc...", TextUtil.truncate("abcdef", 3));
        assertEquals("ab", TextUtil.truncate("ab", 3));
        assertEquals("", TextUtil.truncate(null, 3));
        assertEquals("", TextUtil.truncate("abc", 0));
    }
}
