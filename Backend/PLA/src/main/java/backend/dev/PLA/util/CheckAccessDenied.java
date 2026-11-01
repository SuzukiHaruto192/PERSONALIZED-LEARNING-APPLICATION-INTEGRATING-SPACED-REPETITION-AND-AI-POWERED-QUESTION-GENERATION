package backend.dev.PLA.util;

import backend.dev.PLA.exceptions.AppException;
import backend.dev.PLA.exceptions.ErrorCode;
import backend.dev.PLA.security.CustomUserDetail;

import java.util.Objects;

public class CheckAccessDenied {
    public static void checkAccess(CustomUserDetail userDetail , Long id){
        if(!Objects.equals(userDetail.getId(), id)){
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }
}
