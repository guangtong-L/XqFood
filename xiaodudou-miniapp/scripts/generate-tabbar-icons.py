"""将项目自有的简单 SVG 图元转换为 81x81 透明 PNG，不依赖外部版权素材。"""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "src" / "static" / "tabbar-src"
OUTPUT_DIR = ROOT / "src" / "static" / "tabbar"
SCALE = 4
COLORS = {"": "#999999", "-active": "#FF8866"}


def scaled(value: str) -> int:
    return round(float(value) * SCALE)


def points(value: str):
    return [(scaled(x), scaled(y)) for x, y in re.findall(r"([0-9.]+),([0-9.]+)", value)]


def draw_svg(source: Path, color: str) -> Image.Image:
    root = ET.parse(source).getroot()
    image = Image.new("RGBA", (81 * SCALE, 81 * SCALE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    for item in root:
        tag = item.tag.rsplit("}", 1)[-1]
        width = scaled(item.attrib.get("stroke-width", "1"))
        fill = color if item.attrib.get("fill") not in (None, "none") else None
        if tag in ("polygon", "polyline"):
            coords = points(item.attrib["points"])
            if fill: draw.polygon(coords, fill=fill)
            line = coords + ([coords[0]] if tag == "polygon" else [])
            draw.line(line, fill=color, width=width, joint="curve")
        elif tag == "line":
            coords = [(scaled(item.attrib["x1"]), scaled(item.attrib["y1"])),
                      (scaled(item.attrib["x2"]), scaled(item.attrib["y2"]))]
            draw.line(coords, fill=color, width=width)
            if item.attrib.get("stroke-linecap") == "round":
                radius = width // 2
                for x, y in coords:
                    draw.ellipse((x-radius, y-radius, x+radius, y+radius), fill=color)
        elif tag == "rect":
            box = (scaled(item.attrib["x"]), scaled(item.attrib["y"]),
                   scaled(item.attrib["x"]) + scaled(item.attrib["width"]),
                   scaled(item.attrib["y"]) + scaled(item.attrib["height"]))
            draw.rounded_rectangle(box, radius=scaled(item.attrib.get("rx", "0")), fill=fill,
                                   outline=color, width=width)
        elif tag in ("circle", "ellipse"):
            cx, cy = scaled(item.attrib["cx"]), scaled(item.attrib["cy"])
            rx = scaled(item.attrib.get("rx", item.attrib.get("r", "0")))
            ry = scaled(item.attrib.get("ry", item.attrib.get("r", "0")))
            draw.ellipse((cx-rx, cy-ry, cx+rx, cy+ry), fill=fill, outline=color, width=width)
        else:
            raise ValueError(f"Unsupported SVG primitive: {tag}")
    return image.resize((81, 81), Image.Resampling.LANCZOS)


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for name in ("home", "recipe", "me"):
        for suffix, color in COLORS.items():
            draw_svg(SOURCE_DIR / f"{name}.svg", color).save(OUTPUT_DIR / f"{name}{suffix}.png", "PNG")


if __name__ == "__main__":
    main()
