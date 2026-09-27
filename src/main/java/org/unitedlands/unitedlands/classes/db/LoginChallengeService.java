package org.unitedlands.unitedlands.classes.db;

import java.util.UUID;

import org.unitedlands.unitedlands.classes.webservices.LoginChallenge;

import org.unitedlands.libs.ormlite.dao.Dao;

public class LoginChallengeService extends BaseDbService<LoginChallenge> {

    public LoginChallengeService(Dao<LoginChallenge, UUID> dao) {
        super(dao);
    }

    public LoginChallenge getByCode(String code) {
        try {
            return dao.queryForEq("code", code).get(0);
        } catch (Exception ex) {
            return null;
        }
    }

}
