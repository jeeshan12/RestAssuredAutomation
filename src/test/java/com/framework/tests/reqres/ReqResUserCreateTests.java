package com.framework.tests.reqres;

import com.framework.assertions.ApiAssertions;
import com.framework.core.ApiResponse;
import com.framework.services.reqres.model.CreateUserResponse;
import com.framework.services.reqres.model.UserPayload;
import com.framework.utils.TestDataFactory;
import com.framework.tests.base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class ReqResUserCreateTests extends BaseTest {

    @Test(description = "POST /users creates a user and echoes the submitted fields back")
    public void createUser_returnsCreatedUser() {
        UserPayload payload = UserPayload.builder()
                .name(TestDataFactory.randomFullName())
                .job(TestDataFactory.randomJobTitle())
                .build();

        ApiResponse response = userService.createUser(payload);

        ApiAssertions.assertThat(response).hasStatusCode(201);
        CreateUserResponse created = response.as(CreateUserResponse.class);
        Assert.assertEquals(created.getName(), payload.getName());
        Assert.assertEquals(created.getJob(), payload.getJob());
        Assert.assertNotNull(created.getId());
        Assert.assertNotNull(created.getCreatedAt());
    }

    @DataProvider(name = "randomUsers", parallel = true)
    public Object[][] randomUsers() {
        return new Object[][]{
                {UserPayload.builder().name(TestDataFactory.randomFullName()).job(TestDataFactory.randomJobTitle()).build()},
                {UserPayload.builder().name(TestDataFactory.randomFullName()).job(TestDataFactory.randomJobTitle()).build()},
                {UserPayload.builder().name(TestDataFactory.randomFullName()).job(TestDataFactory.randomJobTitle()).build()},
        };
    }

    @Test(description = "Multiple independent users can be created concurrently without interference",
            dataProvider = "randomUsers")
    public void createUser_multipleIndependentPayloads_eachSucceeds(UserPayload payload) {
        ApiResponse response = userService.createUser(payload);

        ApiAssertions.assertThat(response).hasStatusCode(201);
        CreateUserResponse created = response.as(CreateUserResponse.class);
        Assert.assertEquals(created.getName(), payload.getName());
    }
}
