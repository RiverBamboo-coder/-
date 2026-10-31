package com.bri.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link Result} 单元测试（不连数据库）。
 *
 * @author A
 */
public class ResultTest {

    @Test
    public void okShouldCarryData() {
        Result<String> result = Result.ok("蒙内铁路");
        assertTrue(result.isSuccess());
        assertNotNull(result.getMessage());
        assertEquals(Result.DEFAULT_OK_MESSAGE, result.getMessage());
        assertEquals("蒙内铁路", result.getData());
    }

    @Test
    public void okWithoutDataShouldStillBeSuccess() {
        Result<Void> result = Result.ok();
        assertTrue(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    public void okShouldAcceptCustomMessage() {
        Result<Long> result = Result.ok(7L, "新增成功，项目 ID=7");
        assertTrue(result.isSuccess());
        assertEquals("新增成功，项目 ID=7", result.getMessage());
        assertEquals(Long.valueOf(7L), result.getData());
    }

    @Test
    public void failShouldCarryReadableMessage() {
        Result<Void> result = Result.fail("同项目同月只能报一次进度");
        assertFalse(result.isSuccess());
        assertEquals("同项目同月只能报一次进度", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    public void failWithoutMessageShouldFallbackToChineseHint() {
        Result<Void> result = Result.fail(null);
        assertFalse(result.isSuccess());
        assertEquals(Result.DEFAULT_FAIL_MESSAGE, result.getMessage());

        Result<Void> blank = Result.fail("");
        assertEquals(Result.DEFAULT_FAIL_MESSAGE, blank.getMessage());
    }

    @Test
    public void fieldsShouldBeMutableForUiBinding() {
        Result<Integer> result = new Result<>();
        result.setSuccess(true);
        result.setMessage("ok");
        result.setData(3);
        assertTrue(result.isSuccess());
        assertEquals("ok", result.getMessage());
        assertEquals(Integer.valueOf(3), result.getData());
    }
}
