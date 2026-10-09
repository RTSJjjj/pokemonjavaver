class Headtop_Name
  NAME_OPACITY   = 224                    # 名字不透明度（0~255）
  NAME_OFFSET_Y  = -30                    # 名字纵坐标偏移，越大越偏上
  NAME_OFFSET_OY = 8                      # 字体纵坐标偏移，越大越偏下
  NAME_BORDER    = 0                      # 名字阴影方式（0=阴影，1=包边）

  # 配色前缀
  PREFIX_COLOR = {
    "#ss" => Color.new(255, 144,   0),     # 超闪
    "#s"  => Color.new(216, 160,   0),     # 普闪
    "#m"  => Color.new( 78, 110, 242),     # 男
    "#f"  => Color.new(248, 128, 164),     # 女
    "#r"  => Color.new(255,  64,  64),     # 红
    "#g"  => Color.new( 48, 224,  96),     # 绿
    "#b"  => Color.new(  0,  64, 255),     # 蓝
    "#y"  => Color.new(248, 216,   0)      # 黄
  }
  
  # 特殊名字颜色
  SPECIAL_NAME_COLORS = {
    '路比' => Color.new(248, 24, 24)
  }
end