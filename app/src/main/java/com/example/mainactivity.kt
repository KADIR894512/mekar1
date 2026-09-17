package com.example
 
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.ui.MasterPrinterApp
import com.example.ui.MasterPrinterViewModel

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val viewModel = ViewModelProvider(this)[MasterPrinterViewModel::class.java]
    setContent {
      MasterPrinterApp(viewModel = viewModel)
    }
  }
}

