package com.bri.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link Page} 单元测试（不连数据库）：默认值、上下限、offset、orderBy 白名单。
 *
 * @author A
 */
public class PageTest {

    @Test
    public void defaultValuesShouldMatchConvention() {
        Page page = new Page();
        assertEquals(1, page.getPageNo());
        assertEquals(20, page.getPageSize());
        assertNull(page.getOrderBy());
        assertFalse(page.hasOrderBy());
        assertEquals(0, page.getOffset());
    }

    @Test
    public void offsetShouldSkipPreviousPages() {
        assertEquals(0, Page.of(1, 20).getOffset());
        assertEquals(40, Page.of(3, 20).getOffset());
        assertEquals(100, Page.of(11, 10).getOffset());
    }

    @Test
    public void pageNoAndPageSizeShouldBeClamped() {
        Page page = new Page();
        page.setPageNo(0);
        assertEquals(Page.DEFAULT_PAGE_NO, page.getPageNo());
        page.setPageNo(-8);
        assertEquals(Page.DEFAULT_PAGE_NO, page.getPageNo());
        page.setPageNo(Page.MAX_PAGE_NO + 1);
        assertEquals(Page.MAX_PAGE_NO, page.getPageNo());

        page.setPageSize(0);
        assertEquals(Page.MIN_PAGE_SIZE, page.getPageSize());
        page.setPageSize(-3);
        assertEquals(Page.MIN_PAGE_SIZE, page.getPageSize());
        page.setPageSize(99999);
        assertEquals(Page.MAX_PAGE_SIZE, page.getPageSize());
    }

    @Test
    public void blankOrderByMeansNoOrder() {
        Page page = Page.of(1, 20, "   ");
        assertNull(page.getOrderBy());
        assertNull(page.resolveOrderBy("project_code"));
    }

    @Test
    public void shouldBuildOrderByFromWhitelist() {
        Page page = Page.of(1, 20, "project_code desc");
        assertEquals("ORDER BY project_code DESC", page.resolveOrderBy("project_code", "sign_date"));
    }

    @Test
    public void shouldUseWhitelistColumnNameNotUserInput() {
        Page page = Page.of(1, 20, "PROJECT_CODE");
        assertEquals("ORDER BY project_code ASC", page.resolveOrderBy("project_code"));
    }

    @Test
    public void shouldSupportMultipleColumns() {
        Page page = Page.of(1, 20, "sign_date DESC, project_name");
        assertEquals("ORDER BY sign_date DESC, project_name ASC",
                page.resolveOrderBy("sign_date", "project_name"));
    }

    @Test
    public void shouldRejectColumnOutsideWhitelist() {
        Page page = Page.of(1, 20, "project_id;drop");
        try {
            page.resolveOrderBy("project_code");
            fail("白名单外的排序字段必须抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("白名单"));
        }
    }

    @Test
    public void shouldRejectInvalidDirection() {
        Page page = Page.of(1, 20, "project_code sideways");
        try {
            page.resolveOrderBy("project_code");
            fail("非法排序方向必须抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("ASC"));
        }
    }

    @Test
    public void shouldRejectTooManyTokens() {
        Page page = Page.of(1, 20, "project_code DESC extra");
        try {
            page.resolveOrderBy("project_code");
            fail("排序写法错误必须抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("写法"));
        }
    }

    @Test
    public void shouldRejectEmptyWhitelist() {
        Page page = Page.of(1, 20, "project_code");
        try {
            page.resolveOrderBy();
            fail("没有白名单时不允许拼 ORDER BY");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("白名单"));
        }
    }

    @Test
    public void setOrderByShouldNormalizeBlankToNull() {
        Page page = new Page();
        page.setOrderBy("  sign_date DESC  ");
        assertTrue(page.hasOrderBy());
        assertEquals("sign_date DESC", page.getOrderBy());
        page.setOrderBy("   ");
        assertFalse(page.hasOrderBy());
    }
}
