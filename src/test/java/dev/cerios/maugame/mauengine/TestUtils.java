package dev.cerios.maugame.mauengine;

public class TestUtils {
    public static Object getField(Object object, String fieldName) throws Exception {
        var field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }

    public static <T> void setField(T obj, String fieldName, Object value) throws Exception {
        var fieldToSet = obj.getClass().getDeclaredField(fieldName);
        fieldToSet.setAccessible(true);
        fieldToSet.set(obj, value);
    }
}
