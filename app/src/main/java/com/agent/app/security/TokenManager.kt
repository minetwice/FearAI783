package com.agent.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "fearai_agent_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SKEY_KEYGEN,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        prefs.edit().putString("github_pat", token.trim()).apply()
    }

    fun getToken(): String {
        return prefs.getString("github_pat", "") ?: ""
    }

    fun saveRepoOwner(owner: String) {
        prefs.edit().putString("repo_owner", owner.trim()).apply()
    }

    fun getRepoOwner(): String {
        return prefs.getString("repo_owner", "") ?: ""
    }

    fun saveRepoName(repo: String) {
        prefs.edit().putString("repo_name", repo.trim()).apply()
    }

    fun getRepoName(): String {
        return prefs.getString("repo_name", "") ?: ""
    }

    fun saveBaseBranch(branch: String) {
        prefs.edit().putString("base_branch", branch.trim()).apply()
    }

    fun getBaseBranch(): String {
        return prefs.getString("base_branch", "main") ?: "main"
    }
}
