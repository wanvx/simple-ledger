from PIL import Image, ImageDraw

GREEN = (47, 125, 90, 255)
WHITE = (255, 255, 255, 255)
ORANGE = (217, 133, 59, 255)

SCALE = 4.8
SIZE = 108


def px(value):
    return value * SCALE


def render(round_icon=False):
    canvas = Image.new("RGBA", (int(SIZE * SCALE), int(SIZE * SCALE)), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)
    if round_icon:
        draw.ellipse((0, 0, canvas.width, canvas.height), fill=GREEN)
    else:
        draw.rectangle((0, 0, canvas.width, canvas.height), fill=GREEN)
    draw.rounded_rectangle(
        (px(26), px(20), px(82), px(88)),
        radius=px(8),
        fill=WHITE,
    )
    for y, right in ((37, 70), (49, 64), (61, 56)):
        draw.rounded_rectangle(
            (px(38), px(y - 1.6), px(right), px(y + 1.6)),
            radius=px(1.6),
            fill=GREEN,
        )
    draw.ellipse(
        (px(61), px(53), px(87), px(79)),
        fill=ORANGE,
    )
    w = px(3)
    draw.line((px(69), px(59), px(74), px(66)), fill=WHITE, width=int(w))
    draw.line((px(79), px(59), px(74), px(66)), fill=WHITE, width=int(w))
    draw.line((px(74), px(66), px(74), px(74)), fill=WHITE, width=int(w))
    draw.line((px(69), px(68), px(79), px(68)), fill=WHITE, width=int(w))
    return canvas


base = render(False)
round_base = render(True)
sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
root = r"C:\小工具\简单记账\app\src\main\res"
for density, size in sizes.items():
    folder = f"{root}\\mipmap-{density}"
    import os
    os.makedirs(folder, exist_ok=True)
    base.resize((size, size), Image.Resampling.LANCZOS).save(
        f"{folder}\\ic_launcher.png", optimize=True
    )
    round_base.resize((size, size), Image.Resampling.LANCZOS).save(
        f"{folder}\\ic_launcher_round.png", optimize=True
    )
print("icons written")
