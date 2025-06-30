package com.example.txipitapv11;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Clase auxiliar que proporciona una animación visual de clics en pantalla.
 * Muestra un texto animado al hacer clic que se desplaza hacia arriba y se desvanece.
 */
public class ClickAnimationHelper {

    /**
     * Método que crea y ejecuta una animación al hacer clic.
     * Muestra un texto con un valor numérico que sube y se desvanece, luego se elimina de la vista.
     *
     **/
    public static void createClickAnimation(Context context, ViewGroup parent, float x, float y, String value) {

        // Crear y configurar el TextView que se animará.
        TextView animatedText = new TextView(context);
        animatedText.setText("+" + value);
        animatedText.setTextSize(25);
        animatedText.setTextColor(context.getResources().getColor(R.color.white));

        parent.addView(animatedText);

        // Establecer la posición inicial del texto (valores fijos actualmente)
        animatedText.setX(500);
        animatedText.setY(400);

        // Animación de desplazamiento vertical hacia arriba
        ObjectAnimator moveUp = ObjectAnimator.ofFloat(animatedText, "translationY", 700, 470);

        // Animación de desvanecimiento del texto
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(animatedText, "alpha", 1f, 0f);

        // Combinar ambas animaciones en un conjunto
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(moveUp, fadeOut);
        animatorSet.setDuration(1000);

        // Listener para eliminar el TextView del contenedor una vez finalizada la animación
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                parent.removeView(animatedText);
            }
        });

        // Iniciar la animación
        animatorSet.start();
    }
}
