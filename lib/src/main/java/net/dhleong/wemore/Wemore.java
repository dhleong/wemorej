package net.dhleong.wemore;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import rx.Observable;
import rx.schedulers.Schedulers;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

/**
 * Primary interface for interaction with Wemo devices.
 * Example:
 * <code>
 * // toggle all Wemo switches with "Lights" in the name
 * new Wemore().search()
 *     .filter(d -> d.hasFriendlyNameLike("Lights"))
 *     .flatMap(d -> d.toggleBinaryState());
 * </code>
 */
public class Wemore {

    /**
     * The Wemo Switch devices can be turned off and on,
     *  but there are also times where its current state is unknown
     */
    public enum BinaryState {
        UNKNOWN(""),
        ON("1"),
        OFF("0");

        public final String rawValue;
        BinaryState(String rawValue) {
            this.rawValue = rawValue;
        }

        public boolean isKnown() {
            return this != UNKNOWN;
        }

        public static BinaryState fromRaw(final String raw) {
            if (raw == null)
                return UNKNOWN;
            final String trimmed = raw.trim();
            if (trimmed.length() == 0)
                return UNKNOWN;

            return ON.rawValue.equals(trimmed) ? ON : OFF;
        }
    }

    /**
     * Provides information and async interaction with
     *  a discovered Wemo Device. Note that all async
     *  interactions default to using the IO scheduler
     */
    public static class Device {

        static final String BASIC_EVENT_PATH = "/upnp/control/basicevent1";
        static final String SOAP_PAYLOAD = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + "    s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
                + " <s:Body>"
                + "  <u:%s xmlns:u=\"urn:Belkin:service:basicevent:1\">"
                + "   <BinaryState>%s</BinaryState>"
                + "  </u:%s>"
                + " </s:Body>" 
                + "</s:Envelope>";

        static final XPath xPath = XPathFactory.newInstance().newXPath();
        static final XPathExpression sFriendlyName, sBinaryState;
        static {
            try {
                sFriendlyName = xPath.compile("/root/device/friendlyName");
                sBinaryState = xPath
                    .compile("/Envelope/Body/GetBinaryStateResponse/BinaryState");
            } catch (final Exception e) {
                // shouldn't happen
                throw new RuntimeException(e);
            }
        }
        static final DocumentBuilderFactory sDocBuilderFactory = DocumentBuilderFactory
            .newInstance();

        final OkHttpClient okhttp;
        final Ssdp.Info info;

        String friendlyName;
        BinaryState binaryState = BinaryState.UNKNOWN;

        private Device(OkHttpClient okhttp, final Ssdp.Info info) {
            this.okhttp = okhttp;
            this.info = info;
        }

        public String getFriendlyName() {
            return friendlyName;
        }

        /** NB: NOT a regex match */
        public boolean hasFriendlyNameLike(final String input) {
            return friendlyName.trim().contains(input.trim());
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(64);
            builder.append("[DEVICE");

            if (friendlyName != null) {
                builder.append(":").append(friendlyName);
            }

            builder.append("]->\n").append(info);
            return builder.toString();
        }

        public Observable<BinaryState> getBinaryState() {
            if (binaryState.isKnown())
                return Observable.just(binaryState);

            return soapRequest("GetBinaryState", "").map((doc) -> {
                try {
                    String foundBinaryState = sBinaryState.evaluate(doc);
                    if (foundBinaryState != null) {
                        binaryState = BinaryState.fromRaw(foundBinaryState);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return binaryState;
            });
        }

        public Observable<Device> setBinaryState(final BinaryState newState) {
            if (newState == BinaryState.UNKNOWN) {
                throw new IllegalArgumentException("You may not set UNKNOWN state!");
            }
            binaryState = newState;
            return soapRequest("SetBinaryState", newState.rawValue)
                .map(doc -> this); // is this okay...?
        }

        public Observable<Device> toggleBinaryState() {
            return getBinaryState().flatMap((state) -> {
                if (state == BinaryState.ON) {
                    return setBinaryState(BinaryState.OFF);
                } else {
                    // default to turning ON if still unknown
                    return setBinaryState(BinaryState.ON);
                }
            });
        }

        /**
         * This should already have been called by the framework
         *  before any user sees it
         */
        protected Observable<Device> resolve() {

            if (friendlyName != null)
                return Observable.just(this);

            final Observable<Device> obs = Observable.create((subscriber) -> {

                final Request.Builder builder = new Request.Builder()
                    .url(info.location)
                    .get();
                try {
                    final Response resp = okhttp.newCall(builder.build()).execute();

                    final Document doc = sDocBuilderFactory.newDocumentBuilder()
                        .parse(resp.body().byteStream());
                    friendlyName = sFriendlyName.evaluate(doc);

                    subscriber.onNext(this);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            });
            return obs.subscribeOn(Schedulers.io());
        }

        Observable<Document> soapRequest(final String method, final String arg) {
            final Observable<Document> obs = Observable.create((subscriber) -> {
                
                final RequestBody body = RequestBody.create(
                    MediaType.parse("text/xml; charset=\"utf-8\""),
                    soapPayload(method, arg));

                final String url = "http://" + info.host + BASIC_EVENT_PATH;
                final Request.Builder builder = new Request.Builder()
                    .url(url)
                    .post(body)
                    .header("SOAPACTION", 
                        "\"urn:Belkin:service:basicevent:1#" + method + "\"");

                try {
                    final Response resp = okhttp.newCall(builder.build()).execute();
                    final Document doc = sDocBuilderFactory.newDocumentBuilder()
                        .parse(resp.body().byteStream());

                    subscriber.onNext(doc);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            });
            // eclipse whines if I call this directly
            //  above with inline return :(
            return obs.subscribeOn(Schedulers.io());
        }

        static String soapPayload(final String method, final String arg) {
            return String.format(SOAP_PAYLOAD, method, arg, method);
        }
    }

    static final String BELKIN_CONTROLLEE = "urn:Belkin:device:controllee:1";

    static final long DEFAULT_TIMEOUT = 10000;

    final OkHttpClient okhttp;

    private Ssdp ssdp;

    public Wemore() {
        this(null);
    }
    public Wemore(Ssdp ssdp) {
        this.ssdp = ssdp;

        okhttp = new OkHttpClient();
    }

    public Observable<Device> search() {
        return search(DEFAULT_TIMEOUT);
    }

    public Observable<Device> search(final long timeout) {
        final Ssdp lastSsdp = this.ssdp;
        final Ssdp ssdp;
        if (lastSsdp == null) {
            try {
                ssdp = new Ssdp();
                this.ssdp = ssdp; // save
            } catch (final IOException e) {
                return Observable.error(e);
            }
        } else {
            ssdp = lastSsdp;
        }

        return ssdp.discover(BELKIN_CONTROLLEE, timeout)
            .flatMap(info -> new Device(okhttp, info).resolve());
    }
}
