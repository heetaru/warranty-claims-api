package com.stanislav.warrantyclaims.soap;

public interface WarrantyGateway {
    WarrantyCheck check(String warrantyNumber);
}

