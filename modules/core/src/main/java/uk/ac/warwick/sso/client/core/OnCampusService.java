package uk.ac.warwick.sso.client.core;


public interface OnCampusService {
    boolean isOnCampus(HttpRequest request);
    boolean isOnCampus(String remoteAddr);
}
