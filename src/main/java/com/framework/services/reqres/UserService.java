package com.framework.services.reqres;

import com.framework.core.ApiResponse;
import com.framework.services.reqres.model.UserPayload;


public interface UserService {

    ApiResponse listUsers(int page);

    ApiResponse getUser(int id);

    ApiResponse createUser(UserPayload payload);

    ApiResponse updateUser(int id, UserPayload payload);

    ApiResponse deleteUser(int id);
}
