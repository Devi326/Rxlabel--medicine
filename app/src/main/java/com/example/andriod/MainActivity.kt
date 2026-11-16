package com.example.andriod

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.andriod.adapter.RemindersAdapter
import com.example.andriod.adapter.TextLinesAdapter
import com.example.andriod.data.api.OpenFdaApi
import com.example.andriod.data.api.OpenFdaResponse
import com.example.andriod.data.db.AppDatabase
import com.example.andriod.data.db.ReminderEntity
import com.example.andriod.databinding.ActivityMainBinding
import com.example.andriod.util.AlarmScheduler
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var textRecognizer: TextRecognizer
    private lateinit var database: AppDatabase
    private val openFdaApi = OpenFdaApi.create()

    private lateinit var textLinesAdapter: TextLinesAdapter
    private lateinit var remindersAdapter: RemindersAdapter

    // Activity Result Launchers
    private val captureCameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { processImageBitmap(it) }
    }

    private val pickGalleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processImageUri(it) }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInResult(task)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Permission required for full app features", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        setupAuth()
        setupTabs()
        setupOcrAndSearchUi()
        setupRemindersUi()

        checkPermissions()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // ==========================================
    // 1. TAB NAVIGATION
    // ==========================================
    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.containerScan.visibility = View.VISIBLE
                        binding.containerReminders.visibility = View.GONE
                        binding.containerAccount.visibility = View.GONE
                    }
                    1 -> {
                        binding.containerScan.visibility = View.GONE
                        binding.containerReminders.visibility = View.VISIBLE
                        binding.containerAccount.visibility = View.GONE
                    }
                    2 -> {
                        binding.containerScan.visibility = View.GONE
                        binding.containerReminders.visibility = View.GONE
                        binding.containerAccount.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // ==========================================
    // 2. OCR & OPENFDA DRUG SEARCH UI
    // ==========================================
    private fun setupOcrAndSearchUi() {
        textLinesAdapter = TextLinesAdapter(emptyList()) { tappedLine ->
            searchOpenFda(tappedLine.trim())
        }
        binding.rvTextLines.layoutManager = LinearLayoutManager(this)
        binding.rvTextLines.adapter = textLinesAdapter

        binding.btnCaptureImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                captureCameraLauncher.launch(null)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnPickImage.setOnClickListener {
            pickGalleryLauncher.launch("image/*")
        }

        binding.btnSearchManual.setOnClickListener {
            val query = binding.etBrandSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                searchOpenFda(query)
            } else {
                Toast.makeText(this, "Please enter a brand name", Toast.LENGTH_SHORT).show()
            }
        }

        // Expandable Cards click listeners
        binding.headerDosage.setOnClickListener {
            toggleExpandableCard(binding.tvDosageContent, binding.arrowDosage)
        }
        binding.headerWarnings.setOnClickListener {
            toggleExpandableCard(binding.tvWarningsContent, binding.arrowWarnings)
        }
        binding.headerDoNotUse.setOnClickListener {
            toggleExpandableCard(binding.tvDoNotUseContent, binding.arrowDoNotUse)
        }
    }

    private fun toggleExpandableCard(contentView: View, arrowView: android.widget.TextView) {
        if (contentView.visibility == View.VISIBLE) {
            contentView.visibility = View.GONE
            arrowView.text = "▼"
        } else {
            contentView.visibility = View.VISIBLE
            arrowView.text = "▲"
        }
    }

    private fun processImageBitmap(bitmap: Bitmap) {
        binding.ivMedicineBox.setImageBitmap(bitmap)
        binding.ivMedicineBox.visibility = View.VISIBLE

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        runTextRecognition(inputImage)
    }

    private fun processImageUri(uri: Uri) {
        try {
            binding.ivMedicineBox.setImageURI(uri)
            binding.ivMedicineBox.visibility = View.VISIBLE

            val inputImage = InputImage.fromFilePath(this, uri)
            runTextRecognition(inputImage)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runTextRecognition(inputImage: InputImage) {
        binding.progressBar.visibility = View.VISIBLE
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                binding.progressBar.visibility = View.GONE
                val lines = mutableListOf<String>()
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val text = line.text.trim()
                        if (text.isNotBlank() && text.length > 2) {
                            lines.add(text)
                        }
                    }
                }

                if (lines.isNotEmpty()) {
                    binding.tvOcrTitle.visibility = View.VISIBLE
                    binding.rvTextLines.visibility = View.VISIBLE
                    textLinesAdapter.updateData(lines)
                } else {
                    binding.tvOcrTitle.visibility = View.GONE
                    binding.rvTextLines.visibility = View.GONE
                    Toast.makeText(this, "No text recognized in image", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "OCR Failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun searchOpenFda(brandName: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.cardDrugResult.visibility = View.GONE
        binding.cardError.visibility = View.GONE

        // Clean query of special characters for openFDA search query format
        val cleanQuery = brandName.replace("\"", "").trim()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Primary search: exact openfda.brand_name match
                val searchQuery = "openfda.brand_name:\"$cleanQuery\""
                val response = openFdaApi.searchDrugLabel(searchQuery)

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.results?.isNotEmpty() == true) {
                        val drug = response.body()!!.results!![0]
                        displayDrugInfo(drug, cleanQuery)
                    } else if (response.code() == 404) {
                        // Retry with fallback fuzzy search if exact match returned 404
                        fallbackSearchOpenFda(cleanQuery)
                    } else {
                        showNoMatchError(cleanQuery)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    showNoMatchError(cleanQuery)
                }
            }
        }
    }

    private suspend fun fallbackSearchOpenFda(cleanQuery: String) {
        try {
            val fallbackQuery = "openfda.brand_name:$cleanQuery*"
            val response = openFdaApi.searchDrugLabel(fallbackQuery)
            withContext(Dispatchers.Main) {
                if (response.isSuccessful && response.body()?.results?.isNotEmpty() == true) {
                    val drug = response.body()!!.results!![0]
                    displayDrugInfo(drug, cleanQuery)
                } else {
                    showNoMatchError(cleanQuery)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showNoMatchError(cleanQuery)
            }
        }
    }

    private fun displayDrugInfo(drug: com.example.andriod.data.api.DrugResult, searchedName: String) {
        binding.cardDrugResult.visibility = View.VISIBLE
        binding.cardError.visibility = View.GONE

        val brand = drug.openfda?.brandName?.joinToString(", ") ?: searchedName
        val generic = drug.openfda?.genericName?.joinToString(", ") ?: "Not specified"
        val manufacturer = drug.openfda?.manufacturerName?.joinToString(", ") ?: "Not specified"

        binding.tvBrandName.text = brand
        binding.tvGenericName.text = "Generic Name: $generic"
        binding.tvManufacturer.text = "Manufacturer: $manufacturer"

        binding.tvPurpose.text = drug.purpose?.joinToString("\n\n") ?: "No purpose information listed."
        binding.tvIndications.text = drug.indicationsAndUsage?.joinToString("\n\n") ?: "No indications information listed."

        binding.tvDosageContent.text = drug.dosageAndAdministration?.joinToString("\n\n") ?: "No dosage instructions listed."
        binding.tvWarningsContent.text = drug.warnings?.joinToString("\n\n") ?: "No warning details listed."
        binding.tvDoNotUseContent.text = drug.doNotUse?.joinToString("\n\n") ?: "No specific 'do not use' warnings listed."

        // Reset expandable cards state
        binding.tvDosageContent.visibility = View.GONE
        binding.arrowDosage.text = "▼"
        binding.tvWarningsContent.visibility = View.GONE
        binding.arrowWarnings.text = "▼"
        binding.tvDoNotUseContent.visibility = View.GONE
        binding.arrowDoNotUse.text = "▼"
    }

    private fun showNoMatchError(query: String) {
        binding.cardDrugResult.visibility = View.GONE
        binding.cardError.visibility = View.VISIBLE
        binding.tvErrorMessage.text = "No matches found for \"$query\" on openFDA.\n\nTip: Try tapping another text line or typing the exact brand name."
    }

    // ==========================================
    // 3. MEDICINE REMINDERS UI & ROOM DB
    // ==========================================
    private fun setupRemindersUi() {
        remindersAdapter = RemindersAdapter(
            reminders = emptyList(),
            onToggleReminder = { reminder, isEnabled ->
                val updated = reminder.copy(isEnabled = isEnabled)
                lifecycleScope.launch(Dispatchers.IO) {
                    database.reminderDao().updateReminder(updated)
                    withContext(Dispatchers.Main) {
                        if (isEnabled) {
                            AlarmScheduler.scheduleAlarm(this@MainActivity, updated)
                        } else {
                            AlarmScheduler.cancelAlarm(this@MainActivity, updated.id)
                        }
                    }
                }
            },
            onDeleteReminder = { reminder ->
                lifecycleScope.launch(Dispatchers.IO) {
                    database.reminderDao().deleteReminder(reminder)
                    withContext(Dispatchers.Main) {
                        AlarmScheduler.cancelAlarm(this@MainActivity, reminder.id)
                        Toast.makeText(this@MainActivity, "Reminder deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        binding.rvReminders.layoutManager = LinearLayoutManager(this)
        binding.rvReminders.adapter = remindersAdapter

        database.reminderDao().getAllReminders().observe(this) { reminders ->
            if (reminders.isNullOrEmpty()) {
                binding.tvEmptyReminders.visibility = View.VISIBLE
                binding.rvReminders.visibility = View.GONE
                remindersAdapter.updateData(emptyList())
            } else {
                binding.tvEmptyReminders.visibility = View.GONE
                binding.rvReminders.visibility = View.VISIBLE
                remindersAdapter.updateData(reminders)
            }
        }

        binding.fabAddReminder.setOnClickListener {
            showAddReminderDialog()
        }
    }

    private fun showAddReminderDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_reminder, null)
        val etMedicineName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etMedicineName)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker)
        val chipGroupDays = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupDays)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveBtn.setOnClickListener {
                val name = etMedicineName.text?.toString()?.trim() ?: ""
                if (name.isEmpty()) {
                    etMedicineName.error = "Please enter medicine name"
                    return@setOnClickListener
                }

                val hour: Int
                val minute: Int
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    hour = timePicker.hour
                    minute = timePicker.minute
                } else {
                    @Suppress("DEPRECATION")
                    hour = timePicker.currentHour
                    @Suppress("DEPRECATION")
                    minute = timePicker.currentMinute
                }

                val selectedDays = mutableListOf<String>()
                for (i in 0 until chipGroupDays.childCount) {
                    val chip = chipGroupDays.getChildAt(i) as? Chip
                    if (chip != null && chip.isChecked) {
                        selectedDays.add(chip.text.toString())
                    }
                }
                val daysString = selectedDays.joinToString(", ")

                val reminder = ReminderEntity(
                    name = name,
                    hour = hour,
                    minute = minute,
                    days = daysString,
                    isEnabled = true
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    val id = database.reminderDao().insertReminder(reminder)
                    val savedReminder = reminder.copy(id = id.toInt())
                    withContext(Dispatchers.Main) {
                        AlarmScheduler.scheduleAlarm(this@MainActivity, savedReminder)
                        Toast.makeText(this@MainActivity, "Reminder set!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    // ==========================================
    // 4. FIREBASE AUTHENTICATION (GOOGLE SIGN-IN)
    // ==========================================
    private fun setupAuth() {
        try {
            auth = FirebaseAuth.getInstance()
        } catch (e: Exception) {
            // Fallback initialization if FirebaseApp is not ready
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id_dummy))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        binding.btnSignOut.setOnClickListener {
            try {
                auth.signOut()
            } catch (_: Exception) {}
            googleSignInClient.signOut().addOnCompleteListener {
                updateAuthUi(null)
            }
        }

        updateAuthUi(auth.currentUser)
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                firebaseAuthWithGoogle(idToken)
            } ?: run {
                updateAuthUiWithAccount(account)
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateAuthUi(user)
                    Toast.makeText(this, "Signed in as ${user?.displayName}", Toast.LENGTH_SHORT).show()
                } else {
                    val account = GoogleSignIn.getLastSignedInAccount(this)
                    updateAuthUiWithAccount(account)
                }
            }
    }

    private fun updateAuthUi(user: com.google.firebase.auth.FirebaseUser?) {
        if (user != null) {
            binding.layoutSignedOut.visibility = View.GONE
            binding.layoutSignedIn.visibility = View.VISIBLE
            binding.tvProfileName.text = user.displayName ?: "User"
            binding.tvProfileEmail.text = user.email ?: ""

            user.photoUrl?.let { photoUri ->
                Glide.with(this)
                    .load(photoUri)
                    .circleCrop()
                    .into(binding.ivProfilePicture)
            }
        } else {
            val account = GoogleSignIn.getLastSignedInAccount(this)
            if (account != null) {
                updateAuthUiWithAccount(account)
            } else {
                binding.layoutSignedOut.visibility = View.VISIBLE
                binding.layoutSignedIn.visibility = View.GONE
            }
        }
    }

    private fun updateAuthUiWithAccount(account: GoogleSignInAccount?) {
        if (account != null) {
            binding.layoutSignedOut.visibility = View.GONE
            binding.layoutSignedIn.visibility = View.VISIBLE
            binding.tvProfileName.text = account.displayName ?: "User"
            binding.tvProfileEmail.text = account.email ?: ""

            account.photoUrl?.let { photoUri ->
                Glide.with(this)
                    .load(photoUri)
                    .circleCrop()
                    .into(binding.ivProfilePicture)
            }
        } else {
            binding.layoutSignedOut.visibility = View.VISIBLE
            binding.layoutSignedIn.visibility = View.GONE
        }
    }
}
