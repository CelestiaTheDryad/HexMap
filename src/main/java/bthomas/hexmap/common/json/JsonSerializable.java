package bthomas.hexmap.common.json;

import com.google.gson.JsonObject;

import java.util.HashSet;

/**
 Interface for converting an object to and from Json representation.
 <p>
 Classes implementing this interface should declare a constructor for creating an object from its Json representation.
 */
public interface JsonSerializable
{
    /**
     Creates an instance of this class from a JSON representation.
     <p>
     Class should either override this method or provide a constructor that takes in a {@link JsonObject JsonObject}

     @param root The JsonObject containing the needed data

     @return The constructed object
     */
    default JsonSerializable fromJson(JsonObject root) throws JsonConversionException
    {
        throw new UnsupportedOperationException("Default JsonSerializable implementation invoked");
    }

    /**
     Builds the information required to store this object into a given root JSON object.

     @param root
     The Json object to store data in
     @param loopDetector
     {@link JsonSerializable#toJson(HashSet) Described in toJson()}

     @throws JsonConversionException
     {@link JsonSerializable#toJson(HashSet) Described in toJson()}
     */
    void buildJson(JsonObject root, HashSet<Object> loopDetector) throws JsonConversionException;

    /**
     Creates and returns a JSON tree representation of this object with loop detection. If the output of this method is
     given to Json constructor for the correct class, then it should create a functionally identical object.
     <p>
     Assuming the {@code loopDetector} is passed on faithfully to all sub-objects, object loops will be detected and
     thrown instead of creating an infinite loop.
     <p>
     There is likely no reason for implementers to override this method.

     @param loopDetector
     The HashSet used to detect conversion loops. When this method is called externally a new set should be
     created. When this method is called from inside a JSON conversion, it should pass on the received set.

     @return A JsonObject that contains the full information needed to rebuild a functional copy of this object

     @throws JsonConversionException
     If a conversion loop is detected or another error is encountered while converting to JSON
     */
    default JsonObject toJson(HashSet<Object> loopDetector) throws JsonConversionException
    {
        if(loopDetector.contains(this))
        {
            throw new JsonConversionException("Json conversion loop detected: " + this);
        }

        JsonObject root = new JsonObject();
        loopDetector.add(this);
        try
        {
            buildJson(root, loopDetector);
        }
        catch(Exception e)
        {
            throw new JsonConversionException("Exception converting to Json", e);
        }
        finally
        {
            loopDetector.remove(this);
        }
        return root;


    }


}
