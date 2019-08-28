package bthomas.hexmap.common.util;

import bthomas.hexmap.common.json.JsonConversionException;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JsonUtils
{
    /**
     Gets a String object from a JsonObject if it is present.

     @param root
     The JsonObject to get the data from.
     @param tag
     The name of the field to retrieve

     @return The String stored in the JSON

     @throws JsonConversionException
     If the field is not present or is not of String type
     */
    public static String getString(JsonObject root, String tag) throws JsonConversionException
    {
        JsonElement obj = root.get(tag);
        if(obj instanceof JsonNull)
        {
            return null;
        }
        else if(obj instanceof JsonPrimitive)
        {
            return obj.getAsString();
        }
        else
        {
            throw new JsonConversionException("No field \"" + tag + "\" defined in Json");
        }
    }

    /**
     Gets a sub-JsonObject object from a JsonObject if it is present.

     @param root
     The JsonObject to get the data from.
     @param tag
     The name of the field to retrieve

     @return The String stored in the JSON

     @throws JsonConversionException
     If the field is not present or is not of Object type
     */
    public static JsonObject getJsonObject(JsonObject root, String tag) throws JsonConversionException
    {
        JsonElement obj = root.get(tag);
        if(obj instanceof JsonNull)
        {
            return null;
        }
        else if(obj instanceof JsonObject)
        {
            return (JsonObject) obj;
        }
        else
        {
            throw new JsonConversionException("No field \"text\" defined in Json");
        }
    }

    /**
     Gets an int from a JsonObject if it is present.

     @param root
     The JsonObject to get the data from.
     @param tag
     The name of the field to retrieve

     @return The String stored in the JSON

     @throws JsonConversionException
     If the field is not present or is not of String type
     */
    public static int getInt(JsonObject root, String tag) throws JsonConversionException
    {
        Number num = getNumber(root, tag);
        if(num == null)
        {
            throw new JsonConversionException("Field \"" + tag + "\" is null");
        }
        return num.intValue();
    }

    /**
     Gets a long from a JsonObject if it is present.

     @param root
     The JsonObject to get the data from.
     @param tag
     The name of the field to retrieve

     @return The String stored in the JSON

     @throws JsonConversionException
     If the field is not present or is not of String type
     */
    public static long getLong(JsonObject root, String tag) throws JsonConversionException
    {
        Number num = getNumber(root, tag);
        if(num == null)
        {
            throw new JsonConversionException("Field \"" + tag + "\" is null");
        }
        return num.longValue();
    }

    /**
     Gets a Number object from a JsonObject if it is present.

     @param root
     The JsonObject to get the data from.
     @param tag
     The name of the field to retrieve

     @return The String stored in the JSON

     @throws JsonConversionException
     If the field is not present or is not of String type
     */
    public static Number getNumber(JsonObject root, String tag) throws JsonConversionException
    {
        JsonElement obj = root.get(tag);
        if(obj instanceof JsonPrimitive)
        {
            JsonPrimitive primitive = (JsonPrimitive) obj;
            if(primitive.isNumber())
            {
                return primitive.getAsNumber();
            }
            else
            {
                throw new JsonConversionException("Field \"" + tag + "\" is not a number");
            }
        }
        else if(obj instanceof JsonNull)
        {
            return null;
        }
        else
        {
            throw new JsonConversionException("No field \"" + tag + "\" defined in Json");
        }
    }
}
