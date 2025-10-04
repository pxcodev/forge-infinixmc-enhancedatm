from PIL import Image, ImageDraw

# Crear imagen de 176x256 píxeles
img = Image.new('RGBA', (176, 256), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Fondo principal del ATM (gris oscuro)
draw.rectangle([0, 0, 175, 165], fill=(60, 63, 65, 255), outline=(40, 40, 40, 255))

# Área del inventario del jugador (gris más claro)
draw.rectangle([7, 166, 169, 249], fill=(120, 120, 120, 255), outline=(80, 80, 80, 255))

# Slots del inventario del jugador (3x9 + hotbar)
for row in range(3):
    for col in range(9):
        x = 8 + col * 18
        y = 166 + row * 18
        draw.rectangle([x, y, x+17, y+17], outline=(60, 60, 60, 255))

# Hotbar
for col in range(9):
    x = 8 + col * 18
    y = 224
    draw.rectangle([x, y, x+17, y+17], outline=(60, 60, 60, 255))

# Campo de entrada de cantidad
draw.rectangle([45, 60, 130, 79], fill=(240, 240, 240, 255), outline=(100, 100, 100, 255))

# Botón de moneda
draw.rectangle([135, 60, 169, 79], fill=(100, 149, 237, 255), outline=(70, 130, 180, 255))

# Líneas divisorias
draw.line([10, 85, 165, 85], fill=(80, 80, 80, 255), width=1)
draw.line([10, 120, 165, 120], fill=(80, 80, 80, 255), width=1)

# Bordes decorativos
draw.rectangle([2, 2, 173, 163], outline=(100, 100, 100, 255), width=1)

# Guardar la imagen
img.save('enhanced_atm.png')
print("Textura creada exitosamente: enhanced_atm.png")