package com.wkclz.auth.contract.infra;

import com.wkclz.auth.bean.AuthMetadata;
import com.wkclz.auth.bean.SubjectAuthorization;

public interface AuthMetadataService {
    AuthMetadata loadMetadata(String appCode);
    SubjectAuthorization loadSubjectAuth(String subjectId, String appCode);
}
