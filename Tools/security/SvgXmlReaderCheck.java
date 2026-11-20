/* SPDX-License-Identifier: GPL-3.0-or-later */
import org.ayuless.security.SvgXmlReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/** Standalone regression checks; no Android runtime or third-party test library. */
public final class SvgXmlReaderCheck {
    private static String parse(byte[] input) throws Exception {
        StringBuilder text = new StringBuilder();
        XMLReader reader = SvgXmlReader.create();
        reader.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] chars, int start, int length) {
                text.append(chars, start, length);
            }
        });
        reader.parse(new InputSource(new ByteArrayInputStream(input)));
        return text.toString();
    }

    private static void reject(String xml, boolean utf16) throws Exception {
        try {
            parse(xml.getBytes(utf16 ? StandardCharsets.UTF_16 : StandardCharsets.UTF_8));
        } catch (SAXException expected) {
            return;
        }
        throw new AssertionError("DTD input was accepted");
    }

    public static void main(String[] args) throws Exception {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg'><text>A &amp; B</text></svg>";
        if (!parse(svg.getBytes(StandardCharsets.UTF_8)).equals("A & B")) {
            throw new AssertionError("Normal SVG text/entity rendering changed");
        }
        if (!parse(svg.getBytes(StandardCharsets.UTF_16)).equals("A & B")) {
            throw new AssertionError("UTF-16 SVG parsing changed");
        }
        Path file = Files.createTempFile("ayuless-xxe-fixture-", ".txt");
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            byte[] data = "EXTERNAL_RESOURCE".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        });
        server.start();
        try {
            Files.writeString(file, "LOCAL_FILE_MARKER");
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/external.dtd";
            reject("<!DOCTYPE svg><svg/>", false);
            reject("<!DOCTYPE svg [<!ENTITY x SYSTEM '" + file.toUri() + "'>]><svg>&x;</svg>", false);
            reject("<!DOCTYPE svg [<!ENTITY x SYSTEM '" + file.toUri() + "'>]><svg>&x;</svg>", true);
            reject("<!DOCTYPE svg SYSTEM '" + url + "'><svg/>", false);
            reject("<!DOCTYPE svg [<!ENTITY % x SYSTEM '" + url + "'>%x;]><svg/>", false);
            reject("<!DOCTYPE svg [<!ENTITY a 'abcd'><!ENTITY b '&a;&a;&a;&a;'>]><svg>&b;</svg>", false);
            if (requests.get() != 0) {
                throw new AssertionError("Parser accessed an external resource");
            }
        } finally {
            server.stop(0);
            Files.deleteIfExists(file);
        }
        System.out.println("SVG XML checks passed: normal UTF-8/UTF-16, blocked DTD/entities, zero HTTP requests.");
    }
}
