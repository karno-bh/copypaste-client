package org.copypaste;

import org.copypaste.util.GetMethodUrlConstructor;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class GetMethodUrlConstructorTest {

    final String url = "http://localhost:8080";
    final String endPoint = "/files";

    @Test
    public void getMethodUrlConstructorTestSimpleTest() {


        GetMethodUrlConstructor constructor = new GetMethodUrlConstructor();
        String construct = constructor.construct(url, endPoint, null);

        Assert.assertEquals(url + endPoint, construct);
    }

    @Test
    public void getMethodUrlConstructorParamsTest() {
        GetMethodUrlConstructor constructor = new GetMethodUrlConstructor();
        Map<String, String> parameters = new HashMap<>();
        parameters.put("file", "alpha beta gamma");
        parameters.put("chunk", "42");
        String construct = constructor.construct(url, endPoint, parameters);
        Assert.assertEquals(url + endPoint + "?file=alpha%20beta%20gamma&chunk=42", construct);
    }
}
