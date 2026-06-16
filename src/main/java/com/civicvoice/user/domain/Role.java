package com.civicvoice.user.domain;

/**
 * Platform roles.
 * CITIZEN  – default role for self-registered users
 * AUTHORITY – government department officials (created by ADMIN)
 * ADMIN    – platform superuser
 */
public enum Role {
    CITIZEN,
    AUTHORITY,
    NGO,
    ADMIN
}
