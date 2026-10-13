package com.example.localdatebase.cloud;

/** Small callback API so other modules do not depend on Firebase Task classes. */
public interface CloudResultCallback<T> {
    void onSuccess(T value);
    void onError(Exception error);
}
