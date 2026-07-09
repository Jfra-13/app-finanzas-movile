package com.example.finanzas_independientes_app.domain.usecase

/**
 * Projects the next points of a numeric series with ordinary least-squares
 * linear regression (a straight trend line through the history).
 *
 * Deliberately simple: a gig worker's monthly net is noisy, so an honest
 * straight-line trend is more defensible than an over-fitted curve. The UI
 * draws the result dashed and labels it "proyección" so it never reads as a
 * promise.
 */
class ProyectarTendenciaUseCase {

    /**
     * @param serie historical values, oldest first (x = 0, 1, 2, ...).
     * @param pasos how many future points to project.
     * @return the projected values for x = n, n+1, ..., n+pasos-1.
     *         Empty when there is not enough history to fit a line (< 2 points).
     */
    operator fun invoke(serie: List<Double>, pasos: Int): List<Double> {
        val n = serie.size
        if (n < 2 || pasos <= 0) return emptyList()

        // Least squares: slope = Σ((x-x̄)(y-ȳ)) / Σ((x-x̄)²), intercept = ȳ - slope·x̄.
        val meanX = (n - 1) / 2.0
        val meanY = serie.average()

        var num = 0.0
        var den = 0.0
        for (x in 0 until n) {
            val dx = x - meanX
            num += dx * (serie[x] - meanY)
            den += dx * dx
        }

        // den == 0 only for a single point, already handled above.
        val slope = num / den
        val intercept = meanY - slope * meanX

        return (n until n + pasos).map { x -> intercept + slope * x }
    }
}
