package com.ugur.iptv.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ugur.iptv.R
import com.ugur.iptv.api.ApiClient
import com.ugur.iptv.data.PreferencesManager
import com.ugur.iptv.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        if (prefs.hasSavedCredentials()) {
            launchMainActivity()
            return
        }

        // Prefill if available
        binding.etServerUrl.setText(prefs.serverUrl)
        binding.etUsername.setText(prefs.username)
        binding.etPassword.setText(prefs.password)
        binding.cbRemember.isChecked = prefs.isRemembered

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val rawServer = binding.etServerUrl.text.toString().trim()
            val user = binding.etUsername.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (rawServer.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                showError(getString(R.string.login_error_fields))
                return@setOnClickListener
            }

            val serverUrl = if (!rawServer.startsWith("http://") && !rawServer.startsWith("https://")) {
                "http://$rawServer"
            } else {
                rawServer
            }

            authenticate(serverUrl, user, pass)
        }

        binding.btnDemo.setOnClickListener {
            // Setup demo mode
            prefs.serverUrl = "http://demo.ugurtv.local"
            prefs.username = "demo"
            prefs.password = "demo"
            prefs.isRemembered = true
            launchMainActivity(isDemo = true)
        }
    }

    private fun authenticate(serverUrl: String, user: String, pass: String) {
        setLoading(true)
        hideError()

        lifecycleScope.launch {
            try {
                val service = ApiClient.getService(serverUrl)
                val response = withContext(Dispatchers.IO) {
                    service.login(user, pass)
                }

                val status = response.userInfo?.status
                if (status == "Active" || response.userInfo?.auth == 1 || response.serverInfo != null) {
                    prefs.serverUrl = serverUrl
                    prefs.username = user
                    prefs.password = pass
                    prefs.isRemembered = binding.cbRemember.isChecked
                    launchMainActivity(isDemo = false)
                } else {
                    val msg = response.userInfo?.message ?: getString(R.string.login_error_connection)
                    showError(msg)
                }
            } catch (e: Exception) {
                showError("${getString(R.string.login_error_connection)} (${e.localizedMessage ?: "Hata"})")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun launchMainActivity(isDemo: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("EXTRA_IS_DEMO", isDemo)
        }
        startActivity(intent)
        finish()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.btnDemo.isEnabled = !isLoading
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }
}
