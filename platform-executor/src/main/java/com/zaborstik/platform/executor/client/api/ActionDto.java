package com.zaborstik.platform.executor.client.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Local mirror of {@code com.zaborstik.platform.api.dto.ActionResponse} —
 * everything the executor needs to translate an {@code actionId} into the
 * {@code system.action.internalname} understood by platform-agent.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionDto {

    public String id;
    public String internalname;
    public String displayname;
    public String description;
    public String metaValue;
    public String actionTypeId;
}
