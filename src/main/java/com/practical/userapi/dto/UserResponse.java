package com.practical.userapi.dto;

import java.util.List;

public class UserResponse {
    private List<UserInfo> users;

    public UserResponse() {}

    public UserResponse(List<UserInfo> users) {
        this.users = users;
    }

    public List<UserInfo> getUsers() { return users; }
    public void setUsers(List<UserInfo> users) { this.users = users; }

    public static class UserInfo {
        private String id;
        private String phone;

        public UserInfo() {}

        public UserInfo(String id, String phone) {
            this.id = id;
            this.phone = phone;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
}