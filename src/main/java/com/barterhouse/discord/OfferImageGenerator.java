package com.barterhouse.discord;

import com.barterhouse.api.TradeOffer;
import com.barterhouse.util.LoggerUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Genera imágenes de ofertas con texturas reales extraídas de los JARs
 */
public class OfferImageGenerator {
    
    private static final int IMAGE_WIDTH = 500;
    private static final int IMAGE_HEIGHT = 300;
    private static final int ITEM_SIZE = 64;
    
    /**
     * Genera una imagen PNG con la información de la oferta
     */
    public static File generateOfferImage(TradeOffer offer, File outputFile) {
        LoggerUtil.info("Generating image for offer - Offered: " + offer.getOfferedItem().getDisplayName().getString() + 
                       ", Requested: " + offer.getRequestedItem().getDisplayName().getString());
        try {
            BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            
            // Antialiasing
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // Fondo con degradado
            GradientPaint gradient = new GradientPaint(0, 0, new Color(44, 47, 51), 0, IMAGE_HEIGHT, new Color(32, 34, 37));
            g.setPaint(gradient);
            g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
            
            // Borde decorativo
            g.setColor(new Color(114, 137, 218));
            g.setStroke(new BasicStroke(4));
            g.drawRoundRect(5, 5, IMAGE_WIDTH - 10, IMAGE_HEIGHT - 10, 20, 20);
            
            // Título
            g.setColor(new Color(255, 255, 255));
            g.setFont(new Font("Arial", Font.BOLD, 28));
            String title = "✨ Nueva Oferta Creada";
            int titleWidth = g.getFontMetrics().stringWidth(title);
            g.drawString(title, (IMAGE_WIDTH - titleWidth) / 2, 45);
            
            // Línea separadora
            g.setColor(new Color(114, 137, 218, 100));
            g.setStroke(new BasicStroke(2));
            g.drawLine(30, 60, IMAGE_WIDTH - 30, 60);
            
            // Información del creador
            g.setColor(new Color(220, 220, 220));
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.drawString("👤 " + offer.getCreatorName(), 30, 95);
            
            // Cajas de items con texturas
            drawItemWithTexture(g, offer.getOfferedItem(), 40, 120, "OFRECE", new Color(87, 242, 135));
            
            // Flecha grande en el medio
            g.setColor(new Color(255, 220, 93));
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("➜", IMAGE_WIDTH / 2 - 20, 180);
            
            drawItemWithTexture(g, offer.getRequestedItem(), IMAGE_WIDTH - 220, 120, "SOLICITA", new Color(237, 66, 69));
            
            g.dispose();
            
            // Guardar imagen
            ImageIO.write(image, "PNG", outputFile);
            LoggerUtil.info("Offer image generated: " + outputFile.getAbsolutePath());
            
            return outputFile;
            
        } catch (Exception e) {
            LoggerUtil.error("Error generating offer image: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private static void drawItemWithTexture(Graphics2D g, ItemStack item, int x, int y, String label, Color accentColor) {
        // Caja con bordes redondeados
        g.setColor(new Color(54, 57, 63));
        g.fill(new RoundRectangle2D.Double(x, y, 180, 140, 15, 15));
        
        // Borde de acento
        g.setColor(accentColor);
        g.setStroke(new BasicStroke(3));
        g.draw(new RoundRectangle2D.Double(x, y, 180, 140, 15, 15));
        
        // Etiqueta superior
        g.setColor(accentColor);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        int labelWidth = g.getFontMetrics().stringWidth(label);
        g.drawString(label, x + (180 - labelWidth) / 2, y + 20);
        
        // Textura del item (centrada)
        BufferedImage texture = loadTextureFromFile(item);
        if (texture != null) {
            Image scaled = texture.getScaledInstance(ITEM_SIZE, ITEM_SIZE, Image.SCALE_FAST);
            g.drawImage(scaled, x + (180 - ITEM_SIZE) / 2, y + 35, null);
        } else {
            // Placeholder si no se encuentra
            g.setColor(new Color(128, 0, 128));
            g.fillRect(x + (180 - ITEM_SIZE) / 2, y + 35, ITEM_SIZE, ITEM_SIZE);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("?", x + 75, y + 75);
        }
        
        // Cantidad sobre la textura
        int actualCount = getActualCount(item);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setStroke(new BasicStroke(3));
        String countText = actualCount + "x";
        g.setColor(Color.BLACK);
        g.drawString(countText, x + 125, y + 90);
        g.setColor(Color.WHITE);
        g.drawString(countText, x + 124, y + 89);
        
        // Nombre del item
        g.setColor(new Color(220, 220, 220));
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        String itemName = item.getDisplayName().getString();
        
        if (itemName.length() > 20) {
            String line1 = itemName.substring(0, Math.min(20, itemName.length()));
            String line2 = itemName.length() > 20 ? itemName.substring(20, Math.min(40, itemName.length())) : "";
            if (line2.length() > 18) line2 = line2.substring(0, 15) + "...";
            
            int line1Width = g.getFontMetrics().stringWidth(line1);
            g.drawString(line1, x + (180 - line1Width) / 2, y + 115);
            
            if (!line2.isEmpty()) {
                int line2Width = g.getFontMetrics().stringWidth(line2);
                g.drawString(line2, x + (180 - line2Width) / 2, y + 130);
            }
        } else {
            int nameWidth = g.getFontMetrics().stringWidth(itemName);
            g.drawString(itemName, x + (180 - nameWidth) / 2, y + 115);
        }
    }
    
    /**
     * Carga la textura desde barterhouse/textures/ o extracted_textures/
     * Estructura esperada: barterhouse/textures/namespace/item_name.png
     */
    private static BufferedImage loadTextureFromFile(ItemStack itemStack) {
        try {
            ResourceLocation itemLocation = net.minecraft.core.Registry.ITEM.getKey(itemStack.getItem());
            if (itemLocation == null) {
                LoggerUtil.warn("ResourceLocation is null for item: " + itemStack);
                return null;
            }
            
            String namespace = itemLocation.getNamespace();
            String itemName = itemLocation.getPath();
            
            Path serverPath = Paths.get("").toAbsolutePath();
            Path textureFile = serverPath.resolve("barterhouse/textures/" + namespace + "/" + itemName + ".png");
            
            // Si no existe, intentar en extracted_textures
            if (!Files.exists(textureFile)) {
                textureFile = serverPath.resolve("extracted_textures/" + namespace + "/" + itemName + ".png");
            }
            
            if (Files.exists(textureFile)) {
                LoggerUtil.info("✓ Texture loaded: " + textureFile);
                return ImageIO.read(textureFile.toFile());
            } else {
                LoggerUtil.warn("✗ Texture not found: " + textureFile);
            }
            
        } catch (IOException e) {
            LoggerUtil.error("Error loading texture: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    private static int getActualCount(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("ActualCount")) {
            return stack.getTag().getInt("ActualCount");
        }
        return stack.getCount();
    }
}
