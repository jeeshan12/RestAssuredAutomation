package com.framework.services.reqres;

import com.framework.core.ApiClient;
import com.framework.core.ApiResponse;
import com.framework.services.BaseService;
import com.framework.services.reqres.model.UserPayload;

import java.util.Map;


public class UserServiceImpl extends BaseService implements UserService {

    public UserServiceImpl(ApiClient apiClient) {
        super(apiClient);
    }

    @Override
    public ApiResponse listUsers(int page) {
        return get(ReqResEndpoints.USERS, null, Map.of("page", page));
    }

    @Override
    public ApiResponse getUser(int id) {
        return get(ReqResEndpoints.USER_BY_ID, Map.of("id", id));
    }

    @Override
    public ApiResponse createUser(UserPayload payload) {
        return post(ReqResEndpoints.USERS, payload);
    }

    @Override
    public ApiResponse updateUser(int id, UserPayload payload) {
        return put(ReqResEndpoints.USER_BY_ID, Map.of("id", id), payload);
    }

    @Override
    public ApiResponse deleteUser(int id) {
        return delete(ReqResEndpoints.USER_BY_ID, Map.of("id", id));
    }
}
