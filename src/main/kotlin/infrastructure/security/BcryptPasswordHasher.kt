package com.myPhysioTime.infrastructure.security

import com.myPhysioTime.domain.ports.PasswordHasher
import org.mindrot.jbcrypt.BCrypt

class BcryptPasswordHasher : PasswordHasher {
    override fun hash(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    override fun verify(password: String, hash: String): Boolean {
        return BCrypt.checkpw(password, hash)
    }
}