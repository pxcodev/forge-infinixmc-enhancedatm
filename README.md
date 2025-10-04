# Enhanced ATM - Complemento para Bubustein Money Mod

## Descripción
Enhanced ATM es un mod complementario para **Bubustein Money 20.1** que activa y mejora la funcionalidad del ATM deshabilitado, proporcionando una interfaz moderna y amigable para realizar operaciones bancarias y conversiones de moneda.

## ✨ Funcionalidades Principales

- **🏧 Functional ATM Interface**: Modern GUI for all banking operations
- **💱 Real-time Currency Exchange**: Convert between all supported currencies with live rates
- **📊 Transaction History**: Track your financial activities
- **⚙️ Configurable Fees**: Different exchange rates based on card type
- **🛡️ Daily Limits**: Optional transaction limits for balanced gameplay
- **🔊 Sound Effects**: Immersive audio feedback for all operations
- **🌍 Multi-language Support**: Full internationalization support (English, Spanish, French, German)

### 🔧 Requirements

- **Minecraft**: 1.20.1
- **Forge**: 47.2.0 or higher
- **BubusteinMoney**: 5.0.0 or higher (required)

### 📦 Installation

1. Install **BubusteinMoney** mod first
2. Download **Enhanced ATM** from releases
3. Place both mod files in your `mods` folder
4. Launch Minecraft with Forge

### 🎮 How to Use

1. **Place an ATM** from BubusteinMoney mod in your world
2. **Craft a key** using the original mod's recipe:
   ```
   [Gold] [Gold] [Diamond]
   [Gold] [    ] [       ]
   [    ] [    ] [       ]
   ```
3. **Hold the key** and **right-click** the ATM
4. **Enhanced ATM interface** will open automatically
5. **Perform banking operations**:
   - **Deposit**: Convert physical money to digital
   - **Withdraw**: Get physical currency from your card
   - **Exchange**: Convert between different currencies

### 💳 Supported Cards

All BubusteinMoney cards are supported with different exchange fees:

| Card Type | Exchange Fee |
|-----------|--------------|
| 🟤 Rusty Card | 15% |
| 🔴 Classic Card | 5% |
| 🟡 Gold Card | 3% |
| ⚫ Steel Card | 1% |
| 💎 Supreme Card | 0% |

### 💰 Supported Currencies

- **EUR** (Euro) - Base currency
- **USD** (US Dollar)
- **GBP** (British Pound)
- **CAD** (Canadian Dollar)
- **RON** (Romanian Leu)
- **MDL** (Moldovan Leu)
- **CHF** (Swiss Franc)
- **AUD** (Australian Dollar)
- **JPY** (Japanese Yen)
- **CZK** (Czech Koruna)
- **MXN** (Mexican Peso)
- And more!

### ⚙️ Configuration

The mod includes extensive configuration options:

```toml
[Exchange Fees]
# Exchange fees for different card types (0.05 = 5%)
rusty_card_exchange_fee = 0.15
classic_card_exchange_fee = 0.05
gold_card_exchange_fee = 0.03
steel_card_exchange_fee = 0.01
supreme_card_exchange_fee = 0.0

[Daily Limits]
enable_daily_limits = true
max_daily_amount = 50000.0

[ATM Behavior]
enable_sound_effects = true
show_transaction_history = true
max_history_entries = 10
```

### 🔄 How It Works

Enhanced ATM uses **reflection** to integrate seamlessly with BubusteinMoney:

1. **Detects ATM blocks** from the original mod
2. **Intercepts interactions** when you have a key
3. **Opens enhanced GUI** instead of the disabled original interface
4. **Executes commands** through BubusteinMoney's existing system
5. **Maintains full compatibility** without modifying original code

### 🌍 Multi-language Support

Enhanced ATM includes **full internationalization support**. All chat messages automatically adapt to the player's Minecraft language setting:

#### Supported Languages:
- 🇺🇸 **English** - Default language
- 🇪🇸 **Spanish** - Complete translation
- 🇫🇷 **French** - Complete translation  
- 🇩🇪 **German** - Complete translation

#### How it works:
- Messages use Minecraft's native localization system
- Players see messages in their configured language automatically
- No additional setup required
- Falls back to English for unsupported languages

#### Message Types:
- ✅ **Success messages** (green) - Deposits, withdrawals, transactions
- ❌ **Error messages** (red) - Insufficient funds, card mismatches
- ⚠️ **Warning messages** (gold) - Currency/denomination fallbacks
- 🚨 **Critical errors** (red + bold) - System errors

For detailed information, see [MULTILINGUAL_SUPPORT.md](MULTILINGUAL_SUPPORT.md)

### 🛠️ For Developers

The mod is designed with clean architecture:

```
com.infinix.enhancedatm/
├── client/screen/          # GUI implementations
├── common/container/       # Server-side menu logic
├── common/network/         # Client-server communication
├── common/events/          # Event handlers
├── common/utils/           # BubusteinMoney integration
└── common/config/          # Configuration management
```

### 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test with BubusteinMoney mod
5. Submit a pull request

### 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### 🙏 Credits

- **BubusteinMoney Team** - For the excellent base mod
- **InfinixMC** - Enhanced ATM development
- **Minecraft Forge** - Modding framework

### 📞 Support

- **Issues**: [GitHub Issues](https://github.com/InfinixMC/EnhancedATM/issues)
- **Discord**: Join our community server
- **Wiki**: [Enhanced ATM Wiki](https://github.com/InfinixMC/EnhancedATM/wiki)

---

**Made with ❤️ by InfinixMC**# forge-infinixmc-enhancedatm
