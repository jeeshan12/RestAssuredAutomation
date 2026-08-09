package com.framework.tests.reqres;

import com.framework.assertions.ApiAssertions;
import com.framework.core.ApiResponse;
import com.framework.services.reqres.model.SingleUserResponse;
import com.framework.services.reqres.model.User;
import com.framework.services.reqres.model.UserListResponse;
import com.framework.tests.base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;


public class ReqResUserApiTests extends BaseTest {

    @Test(description = "GET /users returns a paginated list with the expected shape")
    public void listUsers_returnsPaginatedList() {
        ApiResponse response = userService.listUsers(2);

        ApiAssertions.assertThat(response)
                .hasStatusCode(200)
                .respondsWithin(5000)
                .bodyMatches("page", equalTo(2));

        UserListResponse body = response.as(UserListResponse.class);
        Assert.assertFalse(body.getData().isEmpty(), "Expected at least one user on page 2");
    }

    @Test(description = "GET /users/{id} returns the requested user")
    public void getUser_returnsUser() {
        ApiResponse response = userService.getUser(2);

        ApiAssertions.assertThat(response)
                .hasStatusCode(200)
                .bodyMatches("data.id", equalTo(2))
                .bodyMatches("data.email", notNullValue());

        User user = response.as(SingleUserResponse.class).getData();
        Assert.assertEquals(user.getId(), Integer.valueOf(2));
    }

    @Test(description = "GET /users/{id} for a non-existent user returns 404")
    public void getUser_unknownId_returnsNotFound() {
        ApiResponse response = userService.getUser(9999);

        ApiAssertions.assertThat(response).hasStatusCode(404);
    }
}
