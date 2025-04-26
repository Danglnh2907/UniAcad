package util.service.paymentconfig;

import vn.payos.PayOS;

public class PayOSConfig {

    private static PayOS payOSInstance;

    // Private constructor to prevent instantiation
    private PayOSConfig() {
    }

    // Thread-safe singleton method to get PayOS instance
    public static synchronized PayOS getPayOS() {
        if (payOSInstance == null) {
            // Replace with actual PayOS configuration (e.g., client ID, API key, checksum key)
            payOSInstance = new PayOS("006f70e2-d23e-4c9b-8691-5114365415ab", "5a8b9b6f-ad97-4361-84f7-9b7686390511", "8f7a4d49937f769038e8e68c95ce319e299b093069b9263e73546b3eab4f51ad");
        }
        return payOSInstance;
    }
}