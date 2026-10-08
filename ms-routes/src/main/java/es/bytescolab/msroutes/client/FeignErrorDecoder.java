package es.bytescolab.msroutes.client;

import es.bytescolab.msroutes.exception.DriverNotFoundException;
import es.bytescolab.msroutes.exception.ServiceUnavailableException;
import es.bytescolab.msroutes.exception.VehicleNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;

public class FeignErrorDecoder implements ErrorDecoder {

    private static final String VEHICLE_CLIENT_NAME = "VehicleFeignClient";
    private static final String DRIVER_CLIENT_NAME = "DriverFeignClient";

    private static final String VEHICLE_SERVICE_NAME = "ms-vehicles";
    private static final String DRIVER_SERVICE_NAME = "ms-drivers";

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();

        if (status == 404) {
            if (methodKey.startsWith(VEHICLE_CLIENT_NAME)) {
                return new VehicleNotFoundException();
            }
            if (methodKey.startsWith(DRIVER_CLIENT_NAME)) {
                return new DriverNotFoundException();
            }
        }

        if (status >= 500) {
            return new ServiceUnavailableException(serviceNameFromMethodKey(methodKey),
                    "HTTP " + status);
        }

        return defaultDecoder.decode(methodKey, response);
    }

    private String serviceNameFromMethodKey(String methodKey) {
        if (methodKey == null) {
            return "dependencia";
        }
        if (methodKey.startsWith(VEHICLE_CLIENT_NAME)) {
            return VEHICLE_SERVICE_NAME;
        }
        if (methodKey.startsWith(DRIVER_CLIENT_NAME)) {
            return DRIVER_SERVICE_NAME;
        }
        return "dependencia";
    }

    private static final class Default implements ErrorDecoder {
        @Override
        public Exception decode(String methodKey, Response response) {
            return new RuntimeException("Feign error " + response.status() + " from " + methodKey);
        }
    }
}