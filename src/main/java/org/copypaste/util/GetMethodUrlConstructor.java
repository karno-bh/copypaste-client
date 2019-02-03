package org.copypaste.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Utility class that constructs URL with encoded parameters
 *
 * @author Sergey
 */
public class GetMethodUrlConstructor {

    /**
     *
     * Constructs get URL
     *
     * @param serverUrl
     * @param endPoint must have a slash
     * @param params parameters, if null or empty than ignored.
     * @return
     */
    public String construct(String serverUrl, String endPoint, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(128);
        sb.append(serverUrl).append(endPoint);
        if (params == null || params.isEmpty()) {
            return sb.toString();
        }
        sb.append('?');
        boolean first = true;
        for (Map.Entry<String, String> parameter : params.entrySet()) {
            String prefix;
            if (first) {
                prefix = "";
                first = false;
            } else {
                prefix = "&";
            }
            sb.append(prefix);
            sb.append(parameter.getKey()).append('=').append(encodeURI(parameter.getValue()));
        }
        return sb.toString();
    }

    /**
     * From here: https://stackoverflow.com/questions/607176/java-equivalent-to-javascripts-encodeuricomponent-that-produces-identical-outpu
     *
     * @param s string to be encoded
     * @return encoded URI
     */
    private String encodeURI(String s) {
        String result;
        try {
            result = URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            result = s;
        }
        return result;
    }

}
