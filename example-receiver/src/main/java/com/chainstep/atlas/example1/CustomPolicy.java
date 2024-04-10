package com.chainstep.atlas.example1;

import id.walt.auditor.SimpleVerificationPolicy;
import id.walt.auditor.VerificationPolicyResult;
import id.walt.credentials.w3c.VerifiableCredential;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class CustomPolicy extends SimpleVerificationPolicy {
    public CustomPolicy() {
    }

    @NotNull
    public String getDescription() {
        return "bla bla bla";
    }

    @NotNull
    protected VerificationPolicyResult doVerify(@NotNull VerifiableCredential vc) {
        // Custom verification logic, check the credential and decide if you allow to use the service
        log.info("CustomPolicy verified");
        return VerificationPolicyResult.Companion.success();
    }
}
