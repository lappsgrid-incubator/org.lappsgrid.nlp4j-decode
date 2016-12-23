package org.anc.lapps.nlp4j;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.lappsgrid.discriminator.Discriminators.Uri;

/**
 * @author Alexandru Mahmoud
 */
public class NLP4JCustomDecodeTest
{
    private NLP4JCustomDecode nlp4JCustomDecode;

    @Before
    public void setup()
    {
        nlp4JCustomDecode = new NLP4JCustomDecode();
    }

    @After
    public void cleanup()
    {
        nlp4JCustomDecode = null;

    }
    @Test
    public void testMetadata()
    {
        String jsonMetadata = nlp4JCustomDecode.getMetadata();
        assertNotNull("service.getMetadata() returned null", jsonMetadata);

        Data data = Serializer.parse(jsonMetadata, Data.class);
        assertNotNull("Unable to parse metadata json.", data);
        assertNotSame(data.getPayload().toString(), Discriminators.Uri.ERROR, data.getDiscriminator());

        ServiceMetadata metadata = new ServiceMetadata((Map) data.getPayload());

        assertEquals("Vendor is not correct", "http://www.lappsgrid.org", metadata.getVendor());
        assertEquals("Name is not correct", NLP4JCustomDecode.class.getName(), metadata.getName());
        assertEquals("Version is not correct.","1.0.0-SNAPSHOT" , metadata.getVersion());
        assertEquals("License is not correct", Discriminators.Uri.APACHE2, metadata.getLicense());

        IOSpecification produces = metadata.getProduces();
        assertEquals("Produces encoding is not correct", "UTF-8", produces.getEncoding());
        assertEquals("Too many annotation types produced", 0, produces.getAnnotations().size());
        assertEquals("Too many output formats", 1, produces.getFormat().size());
        assertEquals("LIF not produced", Discriminators.Uri.LAPPS, produces.getFormat().get(0));

        IOSpecification requires = metadata.getRequires();
        assertEquals("Requires encoding is not correct", "UTF-8", requires.getEncoding());
        assertEquals("Requires Discriminator is not correct", Discriminators.Uri.GET, requires.getFormat().get(0));
    }

    @Test
    public void testErrorInput()
    {
        System.out.println("NLP4JCustomDecodeTest.testErrorInput");
        String message = "This is an error message";
        Data<String> data = new Data<>(Uri.ERROR, message);
        String json = nlp4JCustomDecode.execute(data.asJson());
        assertNotNull("No JSON returned from the service", json);

        data = Serializer.parse(json, Data.class);
        assertEquals("Invalid discriminator returned", Uri.ERROR, data.getDiscriminator());
        assertEquals("The error message has changed.", message, data.getPayload());
    }

    @Test
    public void testInvalidDiscriminator()
    {
        System.out.println("NLP4JCustomDecodeTest.testInvalidDiscriminator");
        Data<String> data = new Data<>(Uri.QUERY, "");
        String json = nlp4JCustomDecode.execute(data.asJson());
        assertNotNull("No JSON returned from the service", json);
        data = Serializer.parse(json, Data.class);
        assertEquals("Invalid discriminator returned: " + data.getDiscriminator(), Uri.ERROR, data.getDiscriminator());
        System.out.println(data.getPayload());
    }

    @Test
    public void testExecute()
    {
        System.out.println("NLP4JCustomDecodeTest.testExecute");

        String inputTxt;

        try
        {
            inputTxt = nlp4JCustomDecode.readFile("src/test/resources/text-samples/nlp4j.txt");
        }
        catch (IOException e)
        {
            throw new RuntimeException("A problem occurred in the handling of the test input files.", e);
        }

        Map<String,String> payload = new HashMap<>();
        payload.put("input", inputTxt);
        String jsonPayload = Serializer.toJson(payload);

        Data<String> data = new Data<>(Discriminators.Uri.GET, jsonPayload);

        data.setParameter("ambiguity", "simplified-lowercase");
        data.setParameter("clusters", "brown-simplified-lc");
        data.setParameter("gazetteers", "simplified");
        data.setParameter("pos", "yes");
        data.setParameter("ner", true);

        String response = nlp4JCustomDecode.execute(data.asJson());
        System.out.println(response);
    }

}
