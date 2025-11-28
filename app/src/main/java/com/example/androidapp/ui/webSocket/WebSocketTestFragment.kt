package com.example.androidapp.ui.webSocket

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.androidapp.data.repository.WebSocketRepositoryImpl
import com.example.androidapp.data.remote.WebSocketService
import com.example.androidapp.databinding.FragmentWebsocketTestBinding
import com.google.android.material.snackbar.Snackbar

class WebSocketTestFragment : Fragment() {

    private var _binding: FragmentWebsocketTestBinding? = null
    private val binding get() = _binding!!

    // Create service and repository
    private val webSocketService = WebSocketService("wss://echo.websocket.org")
    private val repository = WebSocketRepositoryImpl(webSocketService)

    // Use ViewModelFactory
    private val viewModel: WebSocketViewModel by viewModels {
        WebSocketViewModelFactory(repository)
    }

    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<MessageItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebsocketTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages)
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupClickListeners() {
        binding.connectButton.setOnClickListener {
            viewModel.connect()
            addSystemMessage("Connecting...")
        }

        binding.disconnectButton.setOnClickListener {
            viewModel.disconnect()
            addSystemMessage("Disconnecting...")
        }

        binding.sendButton.setOnClickListener {
            val message = binding.messageInput.text.toString()
            if (message.isNotBlank()) {
                viewModel.send(message)
                addSentMessage(message)
                binding.messageInput.text?.clear()
            }
        }

        binding.clearButton.setOnClickListener {
            messages.clear()
            messageAdapter.notifyDataSetChanged()
            updateEmptyState()
        }

        binding.messageInput.addTextChangedListener {
            binding.sendButton.isEnabled =
                !it.isNullOrBlank() && viewModel.connectionState.value == ConnectionState.CONNECTED
        }
    }

    private fun observeViewModel() {
        viewModel.connectionState.observe(viewLifecycleOwner) { state ->
            updateConnectionUI(state)
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            addReceivedMessage(msg)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                addSystemMessage("Error: $it")
            }
        }
    }

    private fun updateConnectionUI(state: ConnectionState) {
        when (state) {
            ConnectionState.CONNECTING -> {
                binding.statusText.text = "Connecting..."
                binding.statusText.setTextColor(Color.parseColor("#FF9800"))
                binding.connectButton.isEnabled = false
                binding.disconnectButton.isEnabled = false
                binding.sendButton.isEnabled = false
            }
            ConnectionState.CONNECTED -> {
                binding.statusText.text = "Connected"
                binding.statusText.setTextColor(Color.parseColor("#4CAF50"))
                binding.connectButton.isEnabled = false
                binding.disconnectButton.isEnabled = true
                binding.sendButton.isEnabled = binding.messageInput.text?.isNotBlank() == true
                addSystemMessage("Connected to WebSocket")
            }
            ConnectionState.DISCONNECTED -> {
                binding.statusText.text = "Disconnected"
                binding.statusText.setTextColor(Color.parseColor("#F44336"))
                binding.connectButton.isEnabled = true
                binding.disconnectButton.isEnabled = false
                binding.sendButton.isEnabled = false
                addSystemMessage("Disconnected from WebSocket")
            }
            ConnectionState.ERROR -> {
                binding.statusText.text = "Error"
                binding.statusText.setTextColor(Color.parseColor("#F44336"))
                binding.connectButton.isEnabled = true
                binding.disconnectButton.isEnabled = false
                binding.sendButton.isEnabled = false
            }
        }
    }

    private fun addSentMessage(text: String) {
        messages.add(MessageItem(text, MessageType.SENT, System.currentTimeMillis()))
        messageAdapter.notifyItemInserted(messages.size - 1)
        binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
        updateEmptyState()
    }

    private fun addReceivedMessage(text: String) {
        messages.add(MessageItem(text, MessageType.RECEIVED, System.currentTimeMillis()))
        messageAdapter.notifyItemInserted(messages.size - 1)
        binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
        updateEmptyState()
    }

    private fun addSystemMessage(text: String) {
        messages.add(MessageItem(text, MessageType.SYSTEM, System.currentTimeMillis()))
        messageAdapter.notifyItemInserted(messages.size - 1)
        binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        binding.emptyStateText.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
        binding.messagesRecyclerView.visibility = if (messages.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.disconnect()
        _binding = null
    }
}

// Data classes - ADD THESE AT THE BOTTOM
data class MessageItem(
    val text: String,
    val type: MessageType,
    val timestamp: Long
)

enum class MessageType {
    SENT, RECEIVED, SYSTEM
}