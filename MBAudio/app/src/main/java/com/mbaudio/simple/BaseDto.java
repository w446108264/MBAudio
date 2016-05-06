package com.mbaudio.simple;

import com.google.gson.JsonElement;

import java.io.Serializable;
import java.lang.reflect.Field;

public class BaseDto  implements Serializable {

    private static final long serialVersionUID = 1;
    public transient JsonElement jsonReponse;

    public boolean check() {
        return true;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        result.append(getClass().getName());
        result.append(" Object {");
        result.append(newLine);
        for (Field field : getClass().getDeclaredFields()) {
            result.append("\r\n");
            try {
                result.append(field.getName());
                result.append(": ");
                result.append(field.get(this));
            } catch (IllegalAccessException ex) {
                System.out.println(ex);
            }
            result.append(newLine);
        }
        result.append("}");
        return result.toString();
    }
}
