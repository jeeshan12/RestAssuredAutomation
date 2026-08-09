package com.framework.tests.reqres;

import com.framework.annotations.Serial;
import com.framework.assertions.ApiAssertions;
import com.framework.core.ApiResponse;
import com.framework.services.reqres.model.CreateUserResponse;
import com.framework.services.reqres.model.UpdateUserResponse;
import com.framework.services.reqres.model.UserPayload;
import com.framework.utils.TestDataFactory;
import com.framework.tests.base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;


@Serial(reason = "create -> update -> delete share a mutable userId and must run in order")
public class ReqResUserLifecycleSerialTests extends BaseTest {

    private Integer userId;
    private UserPayload originalPayload;

    @Test(description = "Step 1: create a user and capture its id for later steps", priority = 1)
    public void step1_createUser() {
        originalPayload = UserPayload.builder()
                .name(TestDataFactory.randomFullName())
                .job(TestDataFactory.randomJobTitle())
                .build();

        ApiResponse response = userService.createUser(originalPayload);
        ApiAssertions.assertThat(response).hasStatusCode(201);

        CreateUserResponse created = response.as(CreateUserResponse.class);
        userId = Integer.parseInt(created.getId());
        log.info("Lifecycle test created user id={}", userId);
    }

    @Test(description = "Step 2: update the user created in step 1", priority = 2,
            dependsOnMethods = "step1_createUser")
    public void step2_updateUser() {
        UserPayload updated = UserPayload.builder()
                .name(originalPayload.getName())
                .job("Senior " + originalPayload.getJob())
                .build();

        ApiResponse response = userService.updateUser(userId, updated);

        ApiAssertions.assertThat(response).hasStatusCode(200);
        UpdateUserResponse body = response.as(UpdateUserResponse.class);
        Assert.assertEquals(body.getJob(), updated.getJob());
    }

    @Test(description = "Step 3: delete the user updated in step 2", priority = 3,
            dependsOnMethods = "step2_updateUser")
    public void step3_deleteUser() {
        ApiResponse response = userService.deleteUser(userId);

        ApiAssertions.assertThat(response).hasStatusCode(204);
    }
}
