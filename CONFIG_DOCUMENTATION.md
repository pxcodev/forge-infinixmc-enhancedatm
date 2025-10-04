# Enhanced ATM - Configuración GUI

## 📋 **Descripción General**

Enhanced ATM permite personalizar completamente la interfaz gráfica del cajero automático mediante un sistema de configuración avanzado. Todos los elementos visuales (botones, labels, colores, posiciones) son configurables desde el servidor y se sincronizan automáticamente con todos los clientes.

## 🔧 **Sistema de Configuración**

### **Ubicación del Archivo**
```
config/enhancedatm/gui_config.json
```

### **Recarga de Configuración**
```
/enhancedatm reload
```
*Comando disponible solo para administradores. Recarga y sincroniza la configuración con todos los clientes conectados sin necesidad de reiniciar el servidor.*

---

## 📐 **Elementos Configurables**

### **🔘 Botones**

Cada botón tiene las siguientes propiedades configurables:

| Propiedad | Descripción | Tipo | Ejemplo |
|-----------|-------------|------|---------|
| `offsetX` | Posición horizontal | Número | `10` |
| `offsetY` | Posición vertical | Número | `35` |
| `width` | Ancho del botón | Número | `50` |
| `height` | Alto del botón | Número | `20` |

#### **Botones Disponibles:**
- **`amountField`** - Campo de entrada de cantidad
- **`sourceCurrencyButton`** - Botón "FROM" (divisa origen)
- **`targetCurrencyButton`** - Botón "TO" (divisa destino)
- **`depositButton`** - Botón para depositar
- **`withdrawButton`** - Botón para retirar
- **`detectMoneyButton`** - Botón 💰 para detectar dinero

### **➡️ Símbolo de Conversión**

- **`conversionArrow`** - El símbolo ">" entre botones de divisa
  - `offsetX` - Posición horizontal
  - `offsetY` - Posición vertical

### **🗂️ Grids de Inventario**

#### **Grid de Denominaciones (Dinero)**
- **`denominationGrid`** - Grid 3x3 donde se colocan los billetes para detectar
  - `offsetX` - Posición horizontal de la esquina superior izquierda
  - `offsetY` - Posición vertical de la esquina superior izquierda

#### **Inventario del Jugador**
- **`playerInventory`** - Grid del inventario del jugador (9x4)
  - `offsetX` - Posición horizontal de la esquina superior izquierda
  - `offsetY` - Posición vertical de la esquina superior izquierda
  - **Nota**: Esta funcionalidad está preparada para implementación futura

### **🏷️ Labels de Información**

Cada label tiene propiedades de posición:

| Label | Descripción | Propiedades |
|-------|-------------|-------------|
| `exchangeRateLabel` | Tasa de cambio | `offsetX`, `offsetY` |
| `gridTotalLabel` | Total en grid | `offsetX`, `offsetY` |
| `amountLabel` | Etiqueta de cantidad | `offsetX`, `offsetY` |
| `cardBalanceLabel` | Balance de tarjeta | `offsetX`, `offsetY` |
| `cardSlotLabel` | Etiqueta slot tarjeta | `offsetX`, `offsetY` |
| `titleLabel` | Título del ATM | `offsetX`, `offsetY` |

### **🎨 Colores**

Los colores se configuran en formato decimal o pueden convertirse desde hexadecimal:

| Propiedad | Descripción | Formato |
|-----------|-------------|---------|
| `exchangeRateLabelColor` | Color tasa de cambio | Decimal |
| `gridTotalLabelColor` | Color total grid | Decimal |
| `amountLabelColor` | Color etiqueta cantidad | Decimal |
| `cardBalanceLabelColor` | Color balance tarjeta | Decimal |
| `cardSlotLabelColor` | Color etiqueta slot | Decimal |
| `titleLabelColor` | Color título | Decimal |

**Conversión de Colores:**
- Blanco: `16777215` (hex: `#FFFFFF`)
- Verde: `65280` (hex: `#00FF00`)
- Amarillo: `16776960` (hex: `#FFFF00`)
- Rojo: `16711680` (hex: `#FF0000`)
- Azul: `255` (hex: `#0000FF`)

### **👁️ Visibilidad**

Cada label puede mostrarse u ocultarse:

| Propiedad | Descripción | Tipo |
|-----------|-------------|------|
| `showExchangeRateLabel` | Mostrar tasa de cambio | Boolean |
| `showGridTotalLabel` | Mostrar total grid | Boolean |
| `showAmountLabel` | Mostrar etiqueta cantidad | Boolean |
| `showCardBalanceLabel` | Mostrar balance tarjeta | Boolean |
| `showCardSlotLabel` | Mostrar etiqueta slot | Boolean |
| `showTitleLabel` | Mostrar título | Boolean |

---

## 📝 **Ejemplo de Configuración Completa**

```json
{
  "configVersion": "1.2.0",
  "exchangeRateLabel": {
    "offsetY": 200,
    "offsetX": 5
  },
  "gridTotalLabel": {
    "offsetY": 70,
    "offsetX": 5
  },
  "amountLabel": {
    "offsetY": 166,
    "offsetX": 5
  },
  "cardBalanceLabel": {
    "offsetY": 45,
    "offsetX": 65
  },
  "cardSlotLabel": {
    "offsetY": 15,
    "offsetX": 75
  },
  "titleLabel": {
    "offsetY": 0,
    "offsetX": 88
  },
  "amountField": {
    "offsetX": 5,
    "offsetY": 175,
    "width": 80,
    "height": 20
  },
  "sourceCurrencyButton": {
    "offsetX": 98,
    "offsetY": 175,
    "width": 35,
    "height": 20
  },
  "targetCurrencyButton": {
    "offsetX": 148,
    "offsetY": 175,
    "width": 35,
    "height": 20
  },
  "depositButton": {
    "offsetX": 190,
    "offsetY": 81,
    "width": 50,
    "height": 20
  },
  "withdrawButton": {
    "offsetX": 190,
    "offsetY": 105,
    "width": 50,
    "height": 20
  },
  "detectMoneyButton": {
    "offsetX": 190,
    "offsetY": 130,
    "width": 50,
    "height": 20
  },
  "conversionArrow": {
    "offsetY": 180,
    "offsetX": 137
  },
  "denominationGrid": {
    "offsetX": 6,
    "offsetY": 13
  },
  "playerInventory": {
    "offsetX": 6,
    "offsetY": 83
  },
  "cardSlot": {
    "offsetY": 27,
    "offsetX": 86
  },
  "exchangeRateLabelColor": 16777215,
  "gridTotalLabelColor": 65280,
  "amountLabelColor": 16777215,
  "cardBalanceLabelColor": 16777215,
  "cardSlotLabelColor": 16777215,
  "titleLabelColor": 16777215,
  "showExchangeRateLabel": true,
  "showGridTotalLabel": true,
  "showAmountLabel": true,
  "showCardBalanceLabel": true,
  "showCardSlotLabel": true,
  "showTitleLabel": true
}
```

---

## 🎯 **Casos de Uso Comunes**

### **Mover todos los botones hacia abajo**
```json
{
  "depositButton": {
    "offsetX": 10,
    "offsetY": 55,
    "width": 50,
    "height": 20
  },
  "withdrawButton": {
    "offsetX": 65,
    "offsetY": 55,
    "width": 50,
    "height": 20
  },
  "detectMoneyButton": {
    "offsetX": 125,
    "offsetY": 55,
    "width": 45,
    "height": 20
  }
}
```

### **Reposicionar las grids de inventario**
```json
{
  "denominationGrid": {
    "offsetX": 50,
    "offsetY": 120
  },
  "playerInventory": {
    "offsetX": 20,
    "offsetY": 200
  }
}
```

### **Hacer botones más grandes**
```json
{
  "depositButton": {
    "offsetX": 10,
    "offsetY": 35,
    "width": 70,
    "height": 25
  },
  "withdrawButton": {
    "offsetX": 85,
    "offsetY": 35,
    "width": 70,
    "height": 25
  }
}
```

### **Cambiar colores a tema verde**
```json
{
  "gridTotalLabelColor": 65280,
  "exchangeRateLabelColor": 32768,
  "amountLabelColor": 65280
}
```

### **Ocultar labels innecesarios**
```json
{
  "showExchangeRateLabel": false,
  "showCardSlotLabel": false,
  "showTitleLabel": false
}
```

---

## ⚠️ **Consideraciones Importantes**

### **Limitaciones de Posición**
- **No colocar elementos fuera de la GUI** (coordenadas muy altas/bajas)
- **Evitar solapamientos** entre botones y labels
- **Mantener tamaños mínimos** para legibilidad: ancho ≥ 20px, alto ≥ 18px

### **Formato de Colores**
- Los colores deben estar en **formato decimal**
- Para convertir de hexadecimal a decimal, use una calculadora online
- Ejemplo: `#FF0000` (rojo) = `16711680` (decimal)

### **Sincronización**
- La configuración se almacena en el **servidor**
- Los clientes reciben la configuración automáticamente al conectarse
- Use `/enhancedatm reload` para aplicar cambios inmediatamente

---

## 🚀 **Flujo de Trabajo Recomendado**

1. **Hacer respaldo** del archivo `gui_config.json` actual
2. **Editar configuración** según necesidades
3. **Ejecutar** `/enhancedatm reload` en el servidor
4. **Probar** abriendo el ATM para verificar cambios
5. **Ajustar** si es necesario y repetir pasos 2-4

---

## 🔧 **Funcionalidades del ATM**

### **Conversión Automática**
El botón 💰 (detectMoneyButton) detecta automáticamente todo el dinero en la grid del jugador y lo convierte a la divisa seleccionada, mostrando el total convertido en tiempo real.

### **Labels Informativos**
- **Total Grid**: Muestra el valor total del dinero detectado
- **Conversión**: Muestra la conversión entre divisas seleccionadas
- **Balance**: Muestra el balance de la tarjeta insertada

### **Interfaz Intuitiva**
- Botones claramente etiquetados para cada acción
- Campos de entrada con validación automática
- Feedback visual inmediato de las operaciones

---

*Esta documentación cubre todas las funcionalidades de configuración del Enhanced ATM mod. Para soporte técnico o preguntas adicionales, consulte el código fuente del mod.*