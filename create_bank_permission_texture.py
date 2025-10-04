from PIL import Image, ImageDraw, ImageFont

# Crear imagen de 16x16 píxeles (textura de bloque estándar de Minecraft)
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Color base dorado (como un bloque de oro pero más claro/brillante)
gold_base = (255, 215, 0)  # Dorado brillante
gold_dark = (218, 165, 32)  # Dorado oscuro para sombras
gold_light = (255, 235, 70)  # Dorado muy claro para brillos
green_accent = (50, 205, 50)  # Verde para acento de "permiso/autorizado"

# Fondo dorado
draw.rectangle([0, 0, 15, 15], fill=gold_base)

# Borde oscuro
draw.rectangle([0, 0, 15, 15], outline=gold_dark)

# Añadir algunos píxeles de brillo
for i in range(16):
    for j in range(16):
        # Patrón de brillo diagonal
        if (i + j) % 4 == 0:
            if (i + j) % 8 == 0:
                img.putpixel((i, j), gold_light)
            else:
                img.putpixel((i, j), gold_dark)

# Crear un símbolo de "verificación" o "permiso" en el centro (verde)
# Marca de verificación simplificada
check_points = [
    (5, 7), (6, 7),
    (6, 8), (7, 8),
    (7, 9), (8, 9),
    (8, 8), (9, 8),
    (9, 7), (10, 7),
    (10, 6), (11, 6),
    (11, 5), (12, 5)
]

for point in check_points:
    img.putpixel(point, green_accent)

# Añadir algunos píxeles de brillo a la marca de verificación
bright_green = (144, 238, 144)
img.putpixel((6, 7), bright_green)
img.putpixel((9, 6), bright_green)
img.putpixel((11, 5), bright_green)

# Guardar la textura
output_path = r'src\main\resources\assets\enhancedatm\textures\block\bank_permission_block.png'
img.save(output_path)
print(f"Textura creada exitosamente: {output_path}")
print("Un bloque dorado con marca de verificación verde - simbolizando permiso bancario autorizado")
